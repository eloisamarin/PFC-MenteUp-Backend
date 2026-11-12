package com.umc.menteup.service;

import com.umc.menteup.security.RegraSenha;
import com.umc.menteup.dto.AtualizarUsuarioRequest;
import com.umc.menteup.dto.NovoUsuarioRequest;
import com.umc.menteup.dto.UsuarioListagemResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.Codigo2FA;
import com.umc.menteup.model.EntidadeLog;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioLogin;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class UsuarioService {

    // Código de 2 etapas: vale 5 minutos e aceita até 5 erros (depois disso, só com um código novo)
    static final int VALIDADE_CODIGO_2FA_MINUTOS = 5;
    static final int MAXIMO_TENTATIVAS_2FA = 5;
    // Reenvio: no máximo 3 por login com a senha, com 60 segundos de espera entre um e outro,
    // e só até 15 minutos depois de a senha ter sido conferida
    static final int MAXIMO_REENVIOS_2FA = 3;
    static final int ESPERA_REENVIO_2FA_SEGUNDOS = 60;
    static final int JANELA_REENVIO_2FA_MINUTOS = 15;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private Codigo2FARepository codigo2FARepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private IntensivaoService intensivaoService;

    @Autowired
    private LogSistemaService logSistemaService;

    @Autowired
    private RedefinicaoSenhaService redefinicaoSenhaService;

    private static final Logger log = LoggerFactory.getLogger(UsuarioService.class);

    // Senhas erradas seguidas até bloquear a conta
    static final int MAX_TENTATIVAS_LOGIN = 3;

    private final SecureRandom random = new SecureRandom();

    public List<Usuario> getAll() {
        return usuarioRepository.findAll();
    }

    public Optional<Usuario> getById(Long id) {
        return usuarioRepository.findById(id);
    }

    // LGPD (minimização):
    // - administrador: tudo o que a tela de usuários precisa, inclusive o e-mail;
    // - professor: o e-mail só dos alunos, para escolher o aluno certo ao adicioná-lo na turma;
    // - aluno: só id, nome e tipo de todos.
    public List<UsuarioListagemResponse> listar(String username) {
        Usuario logado = carregarLogado(username);
        boolean administrador = TipoUsuario.ehAdministrador(logado);
        boolean professor = TipoUsuario.ehProfessor(logado);

        return usuarioRepository.findAll().stream()
                .map(usuario -> {
                    if (administrador) return UsuarioListagemResponse.completo(usuario);
                    if (professor && TipoUsuario.ehAluno(usuario)) return UsuarioListagemResponse.comEmail(usuario);
                    return UsuarioListagemResponse.resumido(usuario);
                })
                .toList();
    }

    // Só o próprio usuário ou o administrador consultam os dados de uma pessoa
    public UsuarioListagemResponse buscarPorId(Long id, String username) {
        Usuario logado = carregarLogado(username);

        if (!TipoUsuario.ehAdministrador(logado) && !logado.getId().equals(id)) {
            throw ApiException.proibido(
                    "SOMENTE_PROPRIO_USUARIO", "Você só pode consultar os seus próprios dados.");
        }

        return usuarioRepository.findById(id)
                .map(UsuarioListagemResponse::completo)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    private Usuario carregarLogado(String username) {
        return usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    // Cadastro público (/usuarios/cadastrar): só Aluno ou Professor
    public Optional<Usuario> cadastrarUsuario(Usuario usuario) {
        String tipo = exigirTipoValido(usuario.getTipoUsuario());

        if (!TipoUsuario.permitidoNoCadastroPublico(tipo)) {
            throw ApiException.proibido("ADMINISTRADOR_SOMENTE_POR_ADMINISTRADOR",
                    "Somente um administrador pode criar outro administrador.");
        }

        if (!usuario.isAceitouTermos()) {
            throw ApiException.requisicaoInvalida("TERMO_NAO_ACEITO",
                    "Para criar a conta, é preciso aceitar o Termo de Aceite e a Política de Privacidade.");
        }

        if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent()) {
            return Optional.empty();
        }

        usuario.setTipoUsuario(tipo);
        usuario.setTermoAceitoEm(LocalDateTime.now());
        Usuario salvo = salvarNovo(usuario);

        logSistemaService.registrarSucesso(
                salvo,
                AcaoLog.CADASTRAR_USUARIO,
                EntidadeLog.USUARIO,
                salvo.getId(),
                "Usuário \"" + salvo.getNomeUsuario() + "\" cadastrado.",
                Map.of("tipo", String.valueOf(salvo.getTipoUsuario()))
        );

        return Optional.of(salvo);
    }

    // Criação pelo administrador (POST /usuarios): qualquer tipo, inclusive outro administrador.
    // Sem senha: a pessoa recebe um convite por e-mail para criar a senha e aceitar o termo.
    public Usuario criarUsuarioPorAdministrador(NovoUsuarioRequest pedido, String username) {
        Usuario autor = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (!TipoUsuario.ehAdministrador(autor)) {
            throw ApiException.proibido("SOMENTE_ADMINISTRADOR",
                    "Somente administradores podem criar usuários por aqui.");
        }

        String tipo = exigirTipoValido(pedido.tipoUsuario());
        String email = pedido.usuario().trim();

        if (usuarioRepository.findByUsuario(email).isPresent()) {
            throw ApiException.conflito("EMAIL_JA_CADASTRADO", "Este e-mail já está cadastrado.");
        }

        Usuario usuario = new Usuario();
        usuario.setNomeUsuario(pedido.nomeUsuario().trim());
        usuario.setUsuario(email);
        usuario.setTipoUsuario(tipo);
        // Senha aleatória que ninguém conhece: a conta só é usada depois do primeiro acesso
        usuario.setSenha(java.util.UUID.randomUUID().toString());
        usuario.setPrimeiroAcessoPendente(true);
        Usuario salvo = salvarNovo(usuario);

        redefinicaoSenhaService.enviarConvite(salvo);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.CADASTRAR_USUARIO,
                EntidadeLog.USUARIO,
                salvo.getId(),
                "Usuário \"" + salvo.getNomeUsuario() + "\" (" + tipo + ") criado pelo administrador; convite enviado por e-mail.",
                Map.of("tipo", tipo)
        );

        return salvo;
    }

    private Usuario salvarNovo(Usuario usuario) {
        RegraSenha.exigirTamanhoEmBytes(usuario.getSenha());
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setId(null);
        return usuarioRepository.save(usuario);
    }

    // Aceita só ALUNO, PROFESSOR ou ADMINISTRADOR (sem diferença de maiúsculas) e devolve em maiúsculas
    private static String exigirTipoValido(String tipo) {
        if (!TipoUsuario.ehValido(tipo)) {
            throw ApiException.requisicaoInvalida("TIPO_USUARIO_INVALIDO",
                    "Tipo de usuário inválido. Use ALUNO, PROFESSOR ou ADMINISTRADOR.");
        }
        return TipoUsuario.normalizar(tipo);
    }

    // Edita só nome, e-mail e tipo. O resto (senha, token do celular, avatar, bloqueio,
    // aceite do termo...) continua como está no banco: antes, o corpo inteiro era gravado
    // por cima e apagava esses dados, além de obrigar a reenviar a senha.
    public Optional<Usuario> atualizarUsuario(AtualizarUsuarioRequest dados, String username) {

        if (dados.id() == null) {
            throw ApiException.requisicaoInvalida(
                    "ID_USUARIO_OBRIGATORIO", "O id do usuário é obrigatório para atualizar.");
        }

        Usuario autor = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        boolean administrador = TipoUsuario.ehAdministrador(autor);

        if (!administrador && !autor.getId().equals(dados.id())) {
            throw ApiException.proibido(
                    "SOMENTE_PROPRIO_USUARIO", "Você só pode alterar os seus próprios dados.");
        }

        Optional<Usuario> encontrado = usuarioRepository.findById(dados.id());

        if (encontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario alvo = encontrado.get();

        // Mesmo o administrador só pode usar um dos 3 tipos existentes
        String tipoNovo = exigirTipoValido(dados.tipoUsuario());

        // Só o administrador muda o perfil: sem isso, um aluno poderia se promover a professor
        if (!administrador && !alvo.getTipoUsuario().equalsIgnoreCase(tipoNovo)) {
            throw ApiException.proibido(
                    "ALTERACAO_DE_TIPO_NEGADA", "Somente administradores podem alterar o tipo de usuário.");
        }

        String email = dados.usuario().trim();
        Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuario(email);

        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(alvo.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);
        }

        ///Guarda o perfil antigo para saber se houve troca de permissão
        String tipoAnterior = alvo.getTipoUsuario();

        alvo.setNomeUsuario(dados.nomeUsuario().trim());
        alvo.setUsuario(email);
        alvo.setTipoUsuario(tipoNovo);

        Usuario salvo = usuarioRepository.save(alvo);

        boolean mudouPerfil = tipoAnterior != null && !tipoAnterior.equalsIgnoreCase(salvo.getTipoUsuario());

        logSistemaService.registrarSucesso(
                autor,
                mudouPerfil ? AcaoLog.ALTERACAO_PERMISSAO : AcaoLog.EDITAR_USUARIO,
                EntidadeLog.USUARIO,
                salvo.getId(),
                mudouPerfil
                        ? "Perfil do usuário \"" + salvo.getNomeUsuario() + "\" alterado de " + tipoAnterior + " para " + salvo.getTipoUsuario() + "."
                        : "Usuário \"" + salvo.getNomeUsuario() + "\" atualizado.",
                Map.of(
                        "tipoAnterior", String.valueOf(tipoAnterior),
                        "tipoNovo", String.valueOf(salvo.getTipoUsuario())
                )
        );

        return Optional.of(salvo);
    }

    public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {
        if (usuarioLogin.isEmpty()) {
            return Optional.empty();
        }

        UsuarioLogin login = usuarioLogin.get();
        Optional<Usuario> usuario = usuarioRepository.findByUsuario(login.getUsuario());

        // Conta bloqueada: nem confere a senha
        if (usuario.isPresent() && usuario.get().estaBloqueado()) {
            registrarFalhaLogin(login.getUsuario(), "Tentativa de login com a conta bloqueada.");
            throw contaBloqueada();
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getUsuario(), login.getSenha()));
        } catch (AuthenticationException e) {
            registrarFalhaLogin(login.getUsuario(), "Usuário ou senha inválidos.");
            usuario.ifPresent(this::registrarSenhaErrada);
            return Optional.empty();
        }

        // Acertou a senha: zera o contador
        usuario.ifPresent(this::zerarTentativas);

        if (!gerarEEnviarCodigo2FA(login.getUsuario(), 0)) {
            return Optional.empty();
        }

        UsuarioLogin resposta = new UsuarioLogin();
        resposta.setUsuario(login.getUsuario());
        return Optional.of(resposta); // sem token ainda
    }

    public Optional<UsuarioLogin> validarCodigo2FA(String usuario, String codigo) {
        Optional<Codigo2FA> codigoOpt = codigo2FARepository
                .findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(usuario);

        if (codigoOpt.isEmpty()) {
            registrarFalhaLogin(usuario, "Nenhum código 2FA pendente.");
            throw ApiException.naoAutenticado("CODIGO_2FA_INVALIDO",
                    "Não há código válido. Peça um novo código ou faça o login novamente.");
        }

        Codigo2FA c = codigoOpt.get();

        if (c.getExpiracao().isBefore(LocalDateTime.now())) {
            registrarFalhaLogin(usuario, "Código 2FA expirado.");
            throw ApiException.naoAutenticado("CODIGO_2FA_EXPIRADO",
                    "O código expirou. Peça um novo código.");
        }
        if (!c.getCodigo().equals(codigo)) {
            // Contra força bruta: cada erro conta e, no limite, o código deixa de valer
            int restantes = c.registrarTentativaErrada(MAXIMO_TENTATIVAS_2FA);
            codigo2FARepository.save(c);

            if (restantes == 0) {
                registrarFalhaLogin(usuario, "Código 2FA bloqueado após " + MAXIMO_TENTATIVAS_2FA + " erros.");
                throw ApiException.naoAutenticado("CODIGO_2FA_BLOQUEADO",
                        "Você errou o código " + MAXIMO_TENTATIVAS_2FA + " vezes. Peça um novo código.");
            }

            registrarFalhaLogin(usuario, "Código 2FA incorreto.");
            throw ApiException.naoAutenticado("CODIGO_2FA_INCORRETO",
                    "Código incorreto. " + (restantes == 1
                            ? "Resta 1 tentativa."
                            : "Restam " + restantes + " tentativas."));
        }

        c.setUtilizado(true);
        codigo2FARepository.save(c);

        return usuarioRepository.findByUsuario(usuario)
                .map(u -> {
                    logSistemaService.registrarSucesso(
                            u,
                            AcaoLog.LOGIN,
                            EntidadeLog.USUARIO,
                            u.getId(),
                            "Usuário \"" + u.getNomeUsuario() + "\" entrou no sistema.",
                            null
                    );
                    registrarAcessoIntensivao(u);
                    return construirRespostaLogin(new UsuarioLogin(), u);
                });
    }

    // "Reenviar código": só para quem acabou de conferir a senha no /logar (há um código recente),
    // com espera entre os envios e limite de reenvios. O código anterior deixa de valer.
    public void reenviarCodigo2FA(String usuario) {
        Codigo2FA ultimo = codigo2FARepository.findTopByUsuarioOrderByIdDesc(usuario)
                .orElseThrow(semLoginRecente());

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime geradoEm = ultimo.getExpiracao().minusMinutes(VALIDADE_CODIGO_2FA_MINUTOS);

        if (geradoEm.isBefore(agora.minusMinutes(JANELA_REENVIO_2FA_MINUTOS))) {
            throw semLoginRecente().get();
        }

        if (ultimo.getReenvios() >= MAXIMO_REENVIOS_2FA) {
            throw ApiException.requisicaoInvalida("LIMITE_REENVIO_2FA",
                    "Você já pediu " + MAXIMO_REENVIOS_2FA + " códigos novos. Faça o login novamente.");
        }

        long segundosDesdeOEnvio = java.time.Duration.between(geradoEm, agora).getSeconds();
        if (segundosDesdeOEnvio < ESPERA_REENVIO_2FA_SEGUNDOS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AGUARDE_REENVIO_2FA",
                    "Aguarde " + (ESPERA_REENVIO_2FA_SEGUNDOS - segundosDesdeOEnvio)
                            + " segundos para pedir um novo código.");
        }

        // O código anterior deixa de valer
        ultimo.setUtilizado(true);
        codigo2FARepository.save(ultimo);

        if (!gerarEEnviarCodigo2FA(usuario, ultimo.getReenvios() + 1)) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "FALHA_ENVIO_EMAIL",
                    "Não foi possível enviar o código por e-mail. Tente novamente.");
        }
    }

    private static java.util.function.Supplier<ApiException> semLoginRecente() {
        return () -> ApiException.requisicaoInvalida("SEM_LOGIN_RECENTE",
                "Faça o login com a sua senha novamente para receber um novo código.");
    }

    // Gera o código de 6 dígitos, grava e envia por e-mail. Devolve false se o e-mail falhar.
    private boolean gerarEEnviarCodigo2FA(String usuario, int reenvios) {
        String codigo = String.format("%06d", random.nextInt(1_000_000));

        Codigo2FA novo = new Codigo2FA(usuario, codigo, LocalDateTime.now().plusMinutes(VALIDADE_CODIGO_2FA_MINUTOS));
        novo.setReenvios(reenvios);
        codigo2FARepository.save(novo);

        try {
            emailService.enviarCodigo2FA(usuario, codigo);
            return true;
        } catch (Exception e) {
            registrarFalhaLogin(usuario, "Não foi possível enviar o código 2FA por e-mail.");
            return false;
        }
    }

    ///Conta o dia no Intensivão. Se der erro, o login continua normalmente
    private void registrarAcessoIntensivao(Usuario usuario) {
        if (!TipoUsuario.ehAluno(usuario)) {
            return;
        }
        try {
            intensivaoService.registrarAcesso(usuario.getId());
        } catch (Exception e) {
            log.warn("Falha ao registrar acesso no Intensivão (usuário {}): {}", usuario.getId(), e.getMessage());
        }
    }

    ///Guarda as tentativas de login que deram errado
    private void registrarFalhaLogin(String usuario, String motivo) {
        logSistemaService.registrarPorUsername(
                usuario,
                AcaoLog.FALHA_LOGIN,
                EntidadeLog.USUARIO,
                null,
                ResultadoLog.FALHA,
                motivo,
                null
        );
    }

    public boolean salvarFcmToken(String email, String fcmToken) {
        return usuarioRepository.findByUsuario(email).map(u -> {
            u.setFcmToken(fcmToken);
            usuarioRepository.save(u);
            return true;
        }).orElse(false);
    }

    // Soma uma tentativa errada; na terceira, bloqueia a conta
    private void registrarSenhaErrada(Usuario usuario) {
        int tentativas = usuario.getTentativasLogin() + 1;
        usuario.setTentativasLogin(tentativas);

        if (tentativas < MAX_TENTATIVAS_LOGIN) {
            usuarioRepository.save(usuario);
            return;
        }

        usuario.setBloqueadoEm(LocalDateTime.now());
        usuarioRepository.save(usuario);

        throw contaBloqueada();
    }

    private void zerarTentativas(Usuario usuario) {
        if (usuario.getTentativasLogin() > 0) {
            usuario.setTentativasLogin(0);
            usuarioRepository.save(usuario);
        }
    }

    private ApiException contaBloqueada() {
        return ApiException.proibido("CONTA_BLOQUEADA",
                "Sua senha foi bloqueada após 3 tentativas incorretas. "
                        + "Clique em \"Esqueci minha senha\" para receber no seu e-mail cadastrado "
                        + "um link para criar uma nova senha.");
    }

    public UsuarioLogin construirRespostaLogin(UsuarioLogin usuarioLogin, Usuario usuario) {

        usuarioLogin.setId(usuario.getId());
        usuarioLogin.setNome(usuario.getNomeUsuario());
        usuarioLogin.setUsuario(usuario.getUsuario());
        usuarioLogin.setTipoUsuario(usuario.getTipoUsuario());
        usuarioLogin.setAvatar(usuario.getAvatar() == null ? null : usuario.getAvatar().name());

        usuarioLogin.setToken(gerarToken(usuario.getUsuario()));

        return usuarioLogin;
    }

    private String gerarToken(String usuario) {
        return "Bearer " + jwtService.generateToken(usuario);
    }
}

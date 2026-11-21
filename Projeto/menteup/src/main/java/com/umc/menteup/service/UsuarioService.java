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
import com.umc.menteup.model.StatusCadastro;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioLogin;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.security.JwtService;
import com.umc.menteup.security.RegraIdade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
    private TurmaRepository turmaRepository;

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

    @Autowired
    private DocumentoLegalService documentoLegalService;

    @Autowired
    private Clock clock;

    @Value("${menteup.email.revelar-por-hora:30}")
    private int revelarEmailPorHora;

    private volatile LimitadorDeTentativas limiteRevelarEmail;

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
    // - administrador: tudo o que a tela de usuários precisa, com o e-mail mascarado;
    // - professor: o e-mail mascarado só dos alunos, para escolher o aluno certo ao adicioná-lo na turma;
    // - aluno: só id, nome e tipo dos colegas e professores das turmas dele
    @Transactional(readOnly = true)
    public List<UsuarioListagemResponse> listar(String username) {
        Usuario logado = carregarLogado(username);
        boolean administrador = TipoUsuario.ehAdministrador(logado);
        boolean professor = TipoUsuario.ehProfessor(logado);

        if (!administrador && !professor) {
            return usuarioRepository.findAllById(pessoasDasTurmasDoAluno(logado)).stream()
                    .map(UsuarioListagemResponse::resumido)
                    .toList();
        }

        return usuarioRepository.findAll().stream()
                .map(usuario -> {
                    if (administrador) {
                        return UsuarioListagemResponse.completo(usuario);
                    }
                    if (professor && TipoUsuario.ehAluno(usuario)) {
                        return UsuarioListagemResponse.comEmailMascarado(usuario);
                    }
                    return UsuarioListagemResponse.resumido(usuario);
                })
                .toList();
    }

    // O próprio aluno, os colegas e os professores das turmas em que ele está
    private Set<Long> pessoasDasTurmasDoAluno(Usuario aluno) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(aluno.getId());

        for (Turma turma : turmaRepository.findAllByAlunos_Id(aluno.getId())) {
            turma.getAlunos().forEach(colega -> ids.add(colega.getId()));
            if (turma.getProfessor() != null) {
                ids.add(turma.getProfessor().getId());
            }
        }

        return ids;
    }

    // Só o próprio usuário ou o administrador consultam os dados de uma pessoa
    public UsuarioListagemResponse buscarPorId(Long id, String username) {
        Usuario logado = carregarLogado(username);

        if (!TipoUsuario.ehAdministrador(logado) && !logado.getId().equals(id)) {
            throw ApiException.proibido(
                    "SOMENTE_PROPRIO_USUARIO", "Você só pode consultar os seus próprios dados.");
        }

        return usuarioRepository.findById(id)
                .map(usuario -> respostaPara(logado, usuario))
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));
    }

    // A própria pessoa vê o e-mail inteiro; o administrador, olhando outra pessoa, vê mascarado
    private static UsuarioListagemResponse respostaPara(Usuario logado, Usuario usuario) {
        return logado.getId().equals(usuario.getId())
                ? UsuarioListagemResponse.proprio(usuario)
                : UsuarioListagemResponse.completo(usuario);
    }

    // GET /usuarios/{id}/email: o administrador revela o e-mail de uma pessoa (botão na tela de usuários)
    // Cada consulta fica no log e há um limite por hora, para ninguém copiar todos os e-mails um por um
    @Transactional(readOnly = true)
    public String revelarEmail(Long id, String username) {
        Usuario logado = carregarLogado(username);

        if (!TipoUsuario.ehAdministrador(logado)) {
            throw ApiException.proibido(
                    "SOMENTE_ADMINISTRADOR", "Somente administradores podem ver o e-mail de outra pessoa.");
        }

        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (!limiteRevelarEmail().tentar(String.valueOf(logado.getId()))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "MUITOS_PEDIDOS",
                    "Você atingiu o limite de e-mails revelados por hora. Tente novamente mais tarde.");
        }

        logSistemaService.registrarSucesso(logado, AcaoLog.VISUALIZAR_EMAIL, EntidadeLog.USUARIO, alvo.getId(),
                "Administrador #" + logado.getId() + " visualizou o e-mail do usuário #" + alvo.getId() + ".",
                Map.of("usuarioId", String.valueOf(alvo.getId())));

        return alvo.getUsuario();
    }

    private LimitadorDeTentativas limiteRevelarEmail() {
        LimitadorDeTentativas atual = limiteRevelarEmail;
        if (atual == null) {
            synchronized (this) {
                if (limiteRevelarEmail == null) {
                    limiteRevelarEmail = new LimitadorDeTentativas(revelarEmailPorHora, Duration.ofHours(1), clock);
                }
                atual = limiteRevelarEmail;
            }
        }
        return atual;
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

        // A versão lida precisa ser a que está valendo (senão a pessoa aceitaria um texto antigo)
        documentoLegalService.conferirVersoesInformadas(usuario.getTermoVersao(), usuario.getPoliticaVersao());

        // Aluno precisa ter 12 anos ou mais: a data de nascimento só é conferida, não é guardada
        if (TipoUsuario.ALUNO.equals(tipo)) {
            RegraIdade.exigirIdadeMinima(usuario.getDataNascimento(), LocalDate.now(clock));
            usuario.setIdadeMinimaConfirmadaEm(LocalDateTime.now(clock));
        }

        // RF01, RF03 a RF06 e RF09: senha forte, sem espaço nas pontas
        RegraSenha.validarNovaSenha(usuario.getSenha());

        Optional<Usuario> existente = usuarioRepository.findByUsuario(usuario.getUsuario());

        ///Quem teve o pedido de professor rejeitado pode enviar um novo
        boolean reenvio = existente.isPresent()
                && existente.get().getStatusCadastro() == StatusCadastro.REJEITADA;

        if (existente.isPresent() && !reenvio) {
            return Optional.empty();
        }

        usuario.setTipoUsuario(tipo);
        usuario.setTermoAceitoEm(LocalDateTime.now());
        ///Professor só entra depois que o administrador aprovar
        usuario.setStatusCadastro(TipoUsuario.PROFESSOR.equals(tipo) ? StatusCadastro.PENDENTE : null);
        usuario.setStatusAlteradoEm(usuario.getStatusCadastro() == null ? null : LocalDateTime.now());
        Usuario salvo = reenvio ? reenviarPedido(existente.get(), usuario) : salvarNovo(usuario);
        documentoLegalService.registrarAceiteDasVigentes(salvo, salvo.getTermoAceitoEm());

        logSistemaService.registrarSucesso(
                salvo,
                AcaoLog.CADASTRAR_USUARIO,
                EntidadeLog.USUARIO,
                salvo.getId(),
                salvo.getStatusCadastro() == StatusCadastro.PENDENTE
                        ? "Pedido de cadastro de professor do usuário #" + salvo.getId() + " enviado."
                        : "Usuário #" + salvo.getId() + " cadastrado.",
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
                "Usuário #" + salvo.getId() + " (" + tipo + ") criado pelo administrador;"
                        + " convite enviado por e-mail.",
                Map.of("tipo", tipo)
        );

        return salvo;
    }

    ///Aproveita a conta rejeitada com os dados do novo pedido
    private Usuario reenviarPedido(Usuario conta, Usuario pedido) {
        conta.setNomeUsuario(pedido.getNomeUsuario());
        conta.setTipoUsuario(pedido.getTipoUsuario());
        conta.setSenha(passwordEncoder.encode(pedido.getSenha()));
        conta.setTermoAceitoEm(pedido.getTermoAceitoEm());
        conta.setStatusCadastro(pedido.getStatusCadastro());
        conta.setStatusAlteradoEm(pedido.getStatusAlteradoEm());
        conta.setTentativasLogin(0);
        conta.setBloqueadoEm(null);
        return usuarioRepository.save(conta);
    }

    public UsuarioListagemResponse aprovarProfessor(Long id, String username) {
        return responderPedidoProfessor(id, username, true);
    }

    public UsuarioListagemResponse rejeitarProfessor(Long id, String username) {
        return responderPedidoProfessor(id, username, false);
    }

    ///Administrador aprova ou rejeita o pedido e a pessoa recebe a resposta por e-mail
    private UsuarioListagemResponse responderPedidoProfessor(Long id, String username, boolean aprovar) {
        Usuario autor = carregarLogado(username);

        if (!TipoUsuario.ehAdministrador(autor)) {
            throw ApiException.proibido("SOMENTE_ADMINISTRADOR",
                    "Somente administradores podem responder pedidos de professor.");
        }

        Usuario alvo = usuarioRepository.findById(id)
                .orElseThrow(() -> ApiException.naoEncontrado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        if (alvo.getStatusCadastro() != StatusCadastro.PENDENTE) {
            throw ApiException.conflito("PEDIDO_NAO_PENDENTE",
                    "Este usuário não tem pedido de professor aguardando resposta.");
        }

        alvo.setStatusCadastro(aprovar ? StatusCadastro.APROVADA : StatusCadastro.REJEITADA);
        // A data da rejeição conta o prazo para descartar o pedido
        alvo.setStatusAlteradoEm(LocalDateTime.now());
        Usuario salvo = usuarioRepository.save(alvo);

        logSistemaService.registrarSucesso(
                autor,
                AcaoLog.ALTERACAO_PERMISSAO,
                EntidadeLog.USUARIO,
                salvo.getId(),
                "Pedido de professor do usuário #" + salvo.getId() + " "
                        + (aprovar ? "aprovado." : "rejeitado."),
                Map.of("statusCadastro", salvo.getStatusCadastro().name())
        );

        ///Se o e-mail falhar, a resposta do administrador continua valendo
        try {
            emailService.enviarResultadoCadastroProfessor(salvo.getUsuario(), salvo.getNomeUsuario(), aprovar);
        } catch (Exception e) {
            log.warn("Falha ao avisar o usuário {} sobre o pedido de professor: {}", salvo.getId(), e.getMessage());
        }

        return UsuarioListagemResponse.completo(salvo);
    }

    private Usuario salvarNovo(Usuario usuario) {
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

        // Conta de administrador não perde o acesso por aqui: ninguém rebaixa outro administrador nem a si mesmo
        if (TipoUsuario.ehAdministrador(alvo) && !TipoUsuario.ADMINISTRADOR.equalsIgnoreCase(tipoNovo)) {
            throw ApiException.proibido("REBAIXAR_ADMINISTRADOR_NEGADO",
                    "O tipo de uma conta de administrador não pode ser alterado.");
        }

        // Sem e-mail no pedido, continua o mesmo
        String email = dados.usuario() == null || dados.usuario().isBlank()
                ? alvo.getUsuario()
                : dados.usuario().trim();
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
                        ? "Perfil do usuário #" + salvo.getId() + " alterado de " + tipoAnterior
                                + " para " + salvo.getTipoUsuario() + "."
                        : "Usuário #" + salvo.getId() + " atualizado.",
                Map.of(
                        "tipoAnterior", String.valueOf(tipoAnterior),
                        "tipoNovo", String.valueOf(salvo.getTipoUsuario())
                )
        );

        return Optional.of(salvo);
    }

    // PUT /usuarios/atualizar: a mesma edição, já com a resposta que quem pediu pode ver
    @Transactional
    public Optional<UsuarioListagemResponse> atualizarComResposta(AtualizarUsuarioRequest dados, String username) {
        Usuario logado = carregarLogado(username);
        return atualizarUsuario(dados, username).map(salvo -> respostaPara(logado, salvo));
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

        ///Professor sem aprovação não recebe o código de acesso
        StatusCadastro status = usuario.map(Usuario::getStatusCadastro).orElse(null);
        if (status == StatusCadastro.PENDENTE) {
            registrarFalhaLogin(login.getUsuario(), "Cadastro de professor aguardando aprovação.");
            throw ApiException.proibido("CADASTRO_PENDENTE", "Aguardando aprovação do seu cadastro.");
        }
        if (status == StatusCadastro.REJEITADA) {
            registrarFalhaLogin(login.getUsuario(), "Cadastro de professor rejeitado.");
            throw ApiException.proibido("CADASTRO_REJEITADO",
                    "Seu cadastro como professor não foi aprovado. "
                            + "Você pode enviar um novo pedido pela tela de cadastro.");
        }

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
        if (codigo == null || !passwordEncoder.matches(codigo, c.getCodigo())) {
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
                            "Usuário #" + u.getId() + " entrou no sistema.",
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

        // Vai para o e-mail o código e para o banco só o hash
        Codigo2FA novo = new Codigo2FA(usuario, passwordEncoder.encode(codigo),
                LocalDateTime.now().plusMinutes(VALIDADE_CODIGO_2FA_MINUTOS));
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
        usuario.encerrarSessoes();
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

        usuarioLogin.setToken(gerarToken(usuario));
        usuarioLogin.setAceitesPendentes(documentoLegalService.pendentes(usuario.getId()).stream()
                .map(documento -> documento.getTipo().name())
                .toList());
        usuarioLogin.setIdadePendente(RegraIdade.precisaConfirmar(usuario));

        return usuarioLogin;
    }

    private String gerarToken(Usuario usuario) {
        return "Bearer " + jwtService.generateToken(usuario.getUsuario(), usuario.getVersaoSessao());
    }
}

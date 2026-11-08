package com.umc.menteup.service;

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

    public Optional<Usuario> cadastrarUsuario(Usuario usuario) {

        if (usuarioRepository.findByUsuario(usuario.getUsuario()).isPresent()) {
            return Optional.empty();
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        usuario.setId(null);

        Usuario salvo = usuarioRepository.save(usuario);

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

    public Optional<Usuario> atualizarUsuario(Usuario usuario, String username) {

        if (usuario.getId() == null) {
            throw ApiException.requisicaoInvalida(
                    "ID_USUARIO_OBRIGATORIO", "O id do usuário é obrigatório para atualizar.");
        }

        Usuario autor = usuarioRepository.findByUsuario(username)
                .orElseThrow(() -> ApiException.naoAutenticado(
                        "USUARIO_NAO_ENCONTRADO", "Usuário não encontrado."));

        boolean administrador = TipoUsuario.ehAdministrador(autor);

        if (!administrador && !autor.getId().equals(usuario.getId())) {
            throw ApiException.proibido(
                    "SOMENTE_PROPRIO_USUARIO", "Você só pode alterar os seus próprios dados.");
        }

        Optional<Usuario> alvo = usuarioRepository.findById(usuario.getId());

        if (alvo.isEmpty()) {
            return Optional.empty();
        }

        // Só o administrador muda o perfil: sem isso, um aluno poderia se promover a professor
        if (!administrador && !alvo.get().getTipoUsuario().equalsIgnoreCase(usuario.getTipoUsuario())) {
            throw ApiException.proibido(
                    "ALTERACAO_DE_TIPO_NEGADA", "Somente administradores podem alterar o tipo de usuário.");
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuario(usuario.getUsuario());

        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);
        }

        ///Guarda o perfil antigo para saber se houve troca de permissão
        String tipoAnterior = alvo.get().getTipoUsuario();

        // O corpo da requisição não traz o avatar: mantém o que já estava salvo
        usuario.setAvatar(alvo.get().getAvatar());

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        Usuario salvo = usuarioRepository.save(usuario);

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

        String codigo = String.format("%06d", random.nextInt(1_000_000));

        codigo2FARepository.save(
                new Codigo2FA(login.getUsuario(), codigo, LocalDateTime.now().plusMinutes(5)));

        try {
            emailService.enviarCodigo2FA(login.getUsuario(), codigo);
        } catch (Exception e) {
            registrarFalhaLogin(login.getUsuario(), "Não foi possível enviar o código 2FA por e-mail.");
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
            return Optional.empty();
        }

        Codigo2FA c = codigoOpt.get();

        if (c.getExpiracao().isBefore(LocalDateTime.now())) {
            registrarFalhaLogin(usuario, "Código 2FA expirado.");
            return Optional.empty();
        }
        if (!c.getCodigo().equals(codigo)) {
            registrarFalhaLogin(usuario, "Código 2FA incorreto.");
            return Optional.empty();
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

package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Codigo2FA;
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


import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
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
    private RedefinicaoSenhaService redefinicaoSenhaService;

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


        return Optional.of(usuarioRepository.save(usuario));
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

        // O corpo da requisição não traz o avatar: mantém o que já estava salvo
        usuario.setAvatar(alvo.get().getAvatar());

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return Optional.of(usuarioRepository.save(usuario));
    }

    public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {
        if (usuarioLogin.isEmpty()) {
            return Optional.empty();
        }

        UsuarioLogin login = usuarioLogin.get();
        Optional<Usuario> usuario = usuarioRepository.findByUsuario(login.getUsuario());

        // Conta bloqueada: nem confere a senha
        if (usuario.isPresent() && usuario.get().estaBloqueado()) {
            throw contaBloqueada();
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getUsuario(), login.getSenha()));
        } catch (AuthenticationException e) {
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
            return Optional.empty();
        }

        UsuarioLogin resposta = new UsuarioLogin();
        resposta.setUsuario(login.getUsuario());
        return Optional.of(resposta); // sem token ainda
    }

    public Optional<UsuarioLogin> validarCodigo2FA(String usuario, String codigo) {
        Optional<Codigo2FA> codigoOpt = codigo2FARepository
                .findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(usuario);

        if (codigoOpt.isEmpty()) return Optional.empty();

        Codigo2FA c = codigoOpt.get();

        if (c.getExpiracao().isBefore(LocalDateTime.now())) return Optional.empty();
        if (!c.getCodigo().equals(codigo)) return Optional.empty();

        c.setUtilizado(true);
        codigo2FARepository.save(c);

        return usuarioRepository.findByUsuario(usuario)
                .map(u -> construirRespostaLogin(new UsuarioLogin(), u));
    }

    public boolean salvarFcmToken(String email, String fcmToken) {
        return usuarioRepository.findByUsuario(email).map(u -> {
            u.setFcmToken(fcmToken);
            usuarioRepository.save(u);
            return true;
        }).orElse(false);
    }

    // Soma uma tentativa errada,na terceira bloqueia a conta
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

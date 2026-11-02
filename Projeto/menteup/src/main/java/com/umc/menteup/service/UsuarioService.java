package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Codigo2FA;
import com.umc.menteup.model.TipoUsuario;
import com.umc.menteup.model.Codio2FA;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {

    // Tempo de validade do código 2FA (o e-mail informa 5 minutos)
    private static final int MINUTOS_VALIDADE_CODIGO = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Codigo2FARepository codigo2FARepository;

    @Autowired
    private EmailService emailService;

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

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return Optional.of(usuarioRepository.save(usuario));
    }

    /**
     * Etapa 1 do login: valida usuário e senha, gera um código 2FA
     * e envia por e-mail. NÃO devolve o token JWT — ele só é entregue
     * depois que o código for validado em /usuarios/validar-2fa.
     */
    @Transactional
    public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {
        if (usuarioLogin.isEmpty()) {
            return Optional.empty();
        }

        UsuarioLogin login = usuarioLogin.get();

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getUsuario(), login.getSenha()));
        } catch (Exception e) {
        } catch (AuthenticationException e) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuario(login.getUsuario());

        if (usuarioOpt.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuario = usuarioOpt.get();

        gerarEEnviarCodigo2FA(usuario.getUsuario());

        // Resposta sem token: o front-end deve pedir o código ao usuário
        UsuarioLogin resposta = new UsuarioLogin();
        resposta.setId(usuario.getId());
        resposta.setNome(usuario.getNomeUsuario());
        resposta.setUsuario(usuario.getUsuario());
        resposta.setTipoUsuario(usuario.getTipoUsuario());
        resposta.setSenha(null);
        resposta.setToken(null);

        return Optional.of(resposta);
    }

    /**
     * Etapa 2 do login: confere o código 2FA e, se estiver correto,
     * dentro da validade e ainda não utilizado, devolve o token JWT.
     */
    @Transactional
    public Optional<UsuarioLogin> validarCodigo2FA(String usuario, String codigo) {

        if (usuario == null || usuario.isBlank() || codigo == null || codigo.isBlank()) {
            return Optional.empty();
        }

        Optional<Codio2FA> codigoOpt =
                codigo2FARepository.findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(usuario);

        if (codigoOpt.isEmpty()) {
            return Optional.empty();
        }

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

        Codio2FA codigo2FA = codigoOpt.get();

        if (codigo2FA.getExpiracao().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        if (!codigo2FA.getCodigo().equals(codigo.trim())) {
            return Optional.empty();
        }

        // Código só pode ser usado uma vez
        codigo2FA.setUtilizado(true);
        codigo2FARepository.save(codigo2FA);

        return usuarioRepository.findByUsuario(usuario)
                .map(u -> construirRespostaLogin(new UsuarioLogin(), u));
    }

    /**
     * Salva o token do Firebase Cloud Messaging do usuário logado,
     * usado para enviar notificações push.
     */
    @Transactional
    public boolean salvarFcmToken(String email, String fcmToken) {

        Optional<Usuario> usuarioOpt = usuarioRepository.findByUsuario(email);

        if (usuarioOpt.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setFcmToken(fcmToken);
        usuarioRepository.save(usuario);

        return true;
    }

    public UsuarioLogin construirRespostaLogin(UsuarioLogin usuarioLogin, Usuario usuario) {

        usuarioLogin.setId(usuario.getId());
        usuarioLogin.setNome(usuario.getNomeUsuario());
        usuarioLogin.setUsuario(usuario.getUsuario());
        usuarioLogin.setTipoUsuario(usuario.getTipoUsuario());
        usuarioLogin.setSenha(null); // nunca devolver a senha na resposta

        usuarioLogin.setToken(gerarToken(usuario.getUsuario()));

        return usuarioLogin;
    }

    private void gerarEEnviarCodigo2FA(String email) {

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));

        Codio2FA codigo2FA = new Codio2FA(
                email,
                codigo,
                LocalDateTime.now().plusMinutes(MINUTOS_VALIDADE_CODIGO)
        );

        codigo2FARepository.save(codigo2FA);

        try {
            emailService.enviarCodigo2FA(email, codigo);
        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o código de verificação por e-mail.",
                    e
            );
        }
    }

    private String gerarToken(String usuario) {
        return "Bearer " + jwtService.generateToken(usuario);
    }
}

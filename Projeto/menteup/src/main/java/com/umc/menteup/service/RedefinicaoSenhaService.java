package com.umc.menteup.service;

import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TokenRedefinicaoSenha;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.TokenRedefinicaoSenhaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
/*
 * "Esqueci minha senha": gera um link de uso único que vale 30 minutos e quando for troca de senha e desbloqueia a conta.
 É o único jeito de desbloquear depois de 3 senhas erradas.
 */
@Service
public class RedefinicaoSenhaService {

    private static final Logger log = LoggerFactory.getLogger(RedefinicaoSenhaService.class);
    private static final int VALIDADE_MINUTOS = 30;

    private final UsuarioRepository usuarioRepository;
    private final TokenRedefinicaoSenhaRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final Clock clock;
    private final String frontendUrl;
    private final SecureRandom random = new SecureRandom();

    public RedefinicaoSenhaService(
            UsuarioRepository usuarioRepository,
            TokenRedefinicaoSenhaRepository tokenRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            Clock clock,
            @Value("${app.frontend.url:http://localhost:5173}") String frontendUrl) {
        this.usuarioRepository = usuarioRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.clock = clock;
        this.frontendUrl = frontendUrl;
    }

    // POST /usuarios/senha/esqueci: se o e-mail não existir, não faz nada (e a resposta é a mesma)
    @Transactional
    public void solicitar(String email) {
        usuarioRepository.findByUsuario(email)
                .ifPresent(this::enviarLink);
    }

    private void enviarLink(Usuario usuario) {
        // Só o link mais novo vale
        tokenRepository.findAllByUsuario_IdAndUtilizadoFalse(usuario.getId())
                .forEach(antigo -> antigo.setUtilizado(true));

        String token = gerarToken();
        tokenRepository.save(new TokenRedefinicaoSenha(
                usuario, hash(token), LocalDateTime.now(clock).plusMinutes(VALIDADE_MINUTOS)));

        String link = frontendUrl + "/redefinir-senha?token=" + token;

        try {
            emailService.enviarLinkRedefinicaoSenha(usuario.getUsuario(), link);
        } catch (Exception e) {
            // Não interrompe: a resposta não pode revelar se o e-mail existe
            log.warn("Não foi possível enviar o e-mail de redefinição para o usuário {}", usuario.getId(), e);
        }
    }

    // POST /usuarios/senha/redefinir
    @Transactional
    public void redefinir(String token, String novaSenha) {
        TokenRedefinicaoSenha registro = tokenRepository.findByTokenHash(hash(token))
                .filter(t -> !t.isUtilizado())
                .filter(t -> t.getExpiracao().isAfter(LocalDateTime.now(clock)))
                .orElseThrow(() -> ApiException.requisicaoInvalida(
                        "LINK_INVALIDO", "O link é inválido ou expirou. Solicite um novo."));

        Usuario usuario = registro.getUsuario();
        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuario.setTentativasLogin(0);
        usuario.setBloqueadoEm(null);
        usuarioRepository.save(usuario);

        registro.setUtilizado(true);
        tokenRepository.save(registro);
    }

    // 32 bytes aleatórios em Base64 (seguro para URL): impossível de adivinhar
    private String gerarToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    // SHA-256 em hexadecimal: é o que fica salvo no banco
    static String hash(String token) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}
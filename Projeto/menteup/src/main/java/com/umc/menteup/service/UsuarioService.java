package com.umc.menteup.service;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class UsuarioService {

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

        Usuario usuarioSalvo = usuarioRepository.save(usuario);
        gerarCodigo2FA(usuarioSalvo);

        return Optional.of(usuarioSalvo);
    }

    public Optional<Usuario> atualizarUsuario(Usuario usuario) {

        if (!usuarioRepository.findById(usuario.getId()).isPresent()) {
            return Optional.empty();
        }

        Optional<Usuario> usuarioExistente = usuarioRepository.findByUsuario(usuario.getUsuario());

        if (usuarioExistente.isPresent() && !usuarioExistente.get().getId().equals(usuario.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Usuário já existe!", null);
        }

        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return Optional.of(usuarioRepository.save(usuario));
    }

    public Optional<UsuarioLogin> autenticarUsuario(Optional<UsuarioLogin> usuarioLogin) {

        if (!usuarioLogin.isPresent()) {
            return Optional.empty();
        }

        UsuarioLogin login = usuarioLogin.get();

        try {

            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(login.getUsuario(), login.getSenha()));

            Optional<Usuario> usuarioEncontrado = usuarioRepository.findByUsuario(login.getUsuario());

            if ( usuarioEncontrado.isEmpty()){
                return Optional.empty();

            }

            Usuario usuario = usuarioEncontrado.get();
            gerarCodigo2FA(usuario);

            UsuarioLogin resposta = new UsuarioLogin();

            resposta.setId(usuario.getId());
            resposta.setNome(usuario.getNomeUsuario());
            resposta.setUsuario(usuario.getUsuario());
            resposta.setTipoUsuario(usuario.getTipoUsuario());

            resposta.setToken(null);

            return Optional.of(resposta);


        } catch (Exception e) {

            return Optional.empty();
        }
    }

    private void gerarCodigo2FA(Usuario usuario) {

        String codigo = String.format(
                "%06d",
                new Random().nextInt(1_000_000)
        );

        LocalDateTime expiracao =
                LocalDateTime.now().plusMinutes(5);

        Codio2FA codigo2FA =
                new Codio2FA(
                        usuario.getUsuario(),
                        codigo,
                        expiracao
                );

        codigo2FARepository.save(codigo2FA);

        emailService.enviarCodigo2FA(
                usuario.getUsuario(),
                codigo
        );
    }

    public UsuarioLogin construirRespostaLogin(UsuarioLogin usuarioLogin, Usuario usuario) {

        usuarioLogin.setId(usuario.getId());
        usuarioLogin.setNome(usuario.getNomeUsuario());
        usuarioLogin.setUsuario(usuario.getUsuario());
        usuarioLogin.setTipoUsuario(usuario.getTipoUsuario());

        usuarioLogin.setToken(gerarToken(usuario.getUsuario()));

        return usuarioLogin;
    }

    private String gerarToken(String usuario) {
        return "Bearer " + jwtService.generateToken(usuario);
    }

    public Optional<UsuarioLogin> validarCodigo2FA(
            String usuario,
            String codigo
    ) {

        Optional<Codio2FA> codigoEncontrado =
                codigo2FARepository
                        .findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(
                                usuario
                        );

        if (codigoEncontrado.isEmpty()) {
            return Optional.empty();
        }

        Codio2FA codigo2FA =
                codigoEncontrado.get();

        if (codigo2FA.getExpiracao()
                .isBefore(LocalDateTime.now())) {

            return Optional.empty();
        }

        if (!codigo2FA.getCodigo()
                .equals(codigo)) {

            return Optional.empty();
        }

        codigo2FA.setUtilizado(true);

        codigo2FARepository.save(codigo2FA);

        Optional<Usuario> usuarioEncontrado =
                usuarioRepository.findByUsuario(usuario);

        if (usuarioEncontrado.isEmpty()) {
            return Optional.empty();
        }

        Usuario usuarioEntity =
                usuarioEncontrado.get();

        UsuarioLogin resposta =
                new UsuarioLogin();

        resposta.setId(usuarioEntity.getId());
        resposta.setNome(usuarioEntity.getNomeUsuario());
        resposta.setUsuario(usuarioEntity.getUsuario());
        resposta.setTipoUsuario(
                usuarioEntity.getTipoUsuario()
        );

        resposta.setToken(
                "Bearer " +
                        jwtService.generateToken(usuario)
        );

        return Optional.of(resposta);
    }

    public boolean salvarFcmToken(String email, String token) {

        Optional<Usuario> usuarioOptional =
                usuarioRepository.findByUsuario(email);

        if (usuarioOptional.isEmpty()) {
            return false;
        }

        Usuario usuario = usuarioOptional.get();

        usuario.setFcmToken(token);

        usuarioRepository.save(usuario);

        return true;
    }

}

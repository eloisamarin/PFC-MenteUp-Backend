package com.umc.menteup.service;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.model.UsuarioLogin;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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

            return usuarioRepository.findByUsuario(login.getUsuario())
                    .map(usuario -> construirRespostaLogin(login, usuario));

        } catch (Exception e) {

            return Optional.empty();
        }
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
}

package com.umc.menteup.security;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername (String username) throws UsernameNotFoundException{
        if (username == null || username.isEmpty()){
            throw new UsernameNotFoundException("Usuário (e-mail) não pode ser vazio");
        }

        Optional<Usuario> usuario = usuarioRepository.findByUsuario(username);
        if (usuario.isPresent()){
            return new UserDetailsImpl(usuario.get());
        }else{
            throw new UsernameNotFoundException("Usuário não encontrado" + username);
        }
    }
}

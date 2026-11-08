package com.umc.menteup.repository;

import com.umc.menteup.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);

    ///Usado no ranking e na verificação de conquistas ao iniciar
    List<Usuario> findAllByTipoUsuarioIgnoreCase(String tipoUsuario);

}

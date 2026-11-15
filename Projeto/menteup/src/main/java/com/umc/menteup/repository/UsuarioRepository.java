package com.umc.menteup.repository;

import com.umc.menteup.model.StatusCadastro;
import com.umc.menteup.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByUsuario(String usuario);

    ///Usado no ranking e na verificação de conquistas ao iniciar
    List<Usuario> findAllByTipoUsuarioIgnoreCase(String tipoUsuario);

    // Descarte: pedidos de professor rejeitados antes da data limite
    List<Usuario> findAllByStatusCadastroAndStatusAlteradoEmBefore(StatusCadastro status, LocalDateTime limite);

    // Descarte: convites do administrador que nunca viraram conta (sem primeiro acesso) antes da data limite
    List<Usuario> findAllByPrimeiroAcessoPendenteTrueAndCriadoEmBefore(LocalDateTime limite);

}

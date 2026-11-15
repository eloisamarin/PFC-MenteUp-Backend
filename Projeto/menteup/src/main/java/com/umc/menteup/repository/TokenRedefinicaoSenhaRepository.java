package com.umc.menteup.repository;

import com.umc.menteup.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

import java.util.List;
import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    // Links ainda não usados do usuário: são invalidados quando um novo é gerado
    List<TokenRedefinicaoSenha> findAllByUsuario_IdAndUtilizadoFalse(Long usuarioId);

    // Descarte: links (de senha e de convite) que venceram antes da data limite, usados ou não
    @Modifying
    @Query("delete from TokenRedefinicaoSenha t where t.expiracao < :limite")
    int apagarVencidosAntesDe(@Param("limite") LocalDateTime limite);
}
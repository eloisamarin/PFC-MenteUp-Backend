package com.umc.menteup.repository;

import com.umc.menteup.model.TokenRedefinicaoSenha;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TokenRedefinicaoSenhaRepository extends JpaRepository<TokenRedefinicaoSenha, Long> {

    Optional<TokenRedefinicaoSenha> findByTokenHash(String tokenHash);

    // Links ainda não usados do usuário: são invalidados quando um novo é gerado
    List<TokenRedefinicaoSenha> findAllByUsuario_IdAndUtilizadoFalse(Long usuarioId);
}
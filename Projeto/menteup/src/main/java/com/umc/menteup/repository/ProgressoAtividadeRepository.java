package com.umc.menteup.repository;

import com.umc.menteup.model.ProgressoAtividade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProgressoAtividadeRepository extends JpaRepository<ProgressoAtividade, Long> {
    Optional<ProgressoAtividade> findByUsuario_IdAndAtividade_Id(
            Long usuarioId,
            Long atividadeId
    );

    List<ProgressoAtividade> findAllByUsuario_IdAndAtividade_IdIn(
            Long usuarioId,
            Collection<Long> atividadeIds
    );

    void deleteByUsuario_IdAndAtividade_Id(
            Long usuarioId,
            Long atividadeId
    );
}

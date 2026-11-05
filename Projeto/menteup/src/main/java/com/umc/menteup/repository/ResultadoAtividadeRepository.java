package com.umc.menteup.repository;

import com.umc.menteup.dto.RankingResponse;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.ResultadoAtividade;
import com.umc.menteup.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResultadoAtividadeRepository extends JpaRepository<ResultadoAtividade, Long> {
    Optional<ResultadoAtividade> findByUsuarioAndAtividade(
            Usuario usuario,
            Atividade atividade
    );

    Optional<ResultadoAtividade> findByUsuario_IdAndAtividade_Id(Long usuarioId, long AtividadeId);

    boolean existsByUsuario_IdAndAtividade_Id(Long usuarioId, long AtividadeId);

    List<ResultadoAtividade> findAllByUsuario_IdAndAtividade_IdIn(Long usuarioId, Collection<Long> atividadeIds);

    @Query ("""
                    SELECT new com.umc.menteup.dto.RankingResponse(
                     r.usuario.id,
                              r.usuario.nomeUsuario,
                              SUM(r.pontuacao),
                              COUNT(r.id)
                          )
                          FROM ResultadoAtividade r
                          WHERE UPPER(r.usuario.tipoUsuario) = 'ALUNO'
                          GROUP BY r.usuario.id, r.usuario.nomeUsuario
                          ORDER BY SUM(r.pontuacao) DESC
            """)
    List<RankingResponse> buscarRanking();
}

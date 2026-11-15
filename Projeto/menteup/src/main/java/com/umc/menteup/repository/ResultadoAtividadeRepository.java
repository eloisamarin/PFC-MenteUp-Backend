package com.umc.menteup.repository;

import com.umc.menteup.dto.ResumoDesempenhoAluno;
import com.umc.menteup.dto.TotalPorAluno;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.ResultadoAtividade;
import com.umc.menteup.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ResultadoAtividadeRepository extends JpaRepository<ResultadoAtividade, Long> {
    Optional<ResultadoAtividade> findByUsuarioAndAtividade(
            Usuario usuario,
            Atividade atividade
    );

    Optional<ResultadoAtividade> findByUsuario_IdAndAtividade_Id(Long usuarioId, long atividadeId);

    boolean existsByUsuario_IdAndAtividade_Id(Long usuarioId, long atividadeId);

    List<ResultadoAtividade> findAllByUsuario_IdAndAtividade_IdIn(Long usuarioId, Collection<Long> atividadeIds);

    ///Pontos e quantidade de atividades de cada aluno, usados no ranking (RankingService)
    @Query("""
        SELECT new com.umc.menteup.dto.TotalPorAluno(r.usuario.id, SUM(r.pontuacao), COUNT(r.id))
        FROM ResultadoAtividade r
        GROUP BY r.usuario.id
        """)
    List<TotalPorAluno> somarPorAluno();

    // Mesma soma, só com as atividades de uma turma (ranking da turma)
    @Query("""
        SELECT new com.umc.menteup.dto.TotalPorAluno(r.usuario.id, SUM(r.pontuacao), COUNT(r.id))
        FROM ResultadoAtividade r
        JOIN r.atividade.turmas t
        WHERE t.id = :turmaId
        GROUP BY r.usuario.id
        """)
    List<TotalPorAluno> somarPorAlunoNaTurma(@Param("turmaId") Long turmaId);

    ///Impede que as questões sejam trocadas depois do envio de algum aluno
    boolean existsByAtividade_Id(Long atividadeId);

    long countByAtividade_Turmas_Id(Long turmaId);

    @Query("""
        SELECT new com.umc.menteup.dto.ResumoDesempenhoAluno(
            r.usuario.id, SUM(r.pontuacao), SUM(r.acertos), COUNT(r.id)
        )
        FROM ResultadoAtividade r
        JOIN r.atividade.turmas t
        WHERE t.id = :turmaId
        GROUP BY r.usuario.id
        """)
    List<ResumoDesempenhoAluno> buscarProgressoDaTurma(@Param("turmaId") Long turmaId);
    List<ResultadoAtividade> findAllByUsuario_IdAndAtividade_Turmas_Id(Long usuarioId, Long turmaId);

    ///Conquistas

    long countByUsuario_Id(Long usuarioId);

    ///Atividades com todas as questões certas
    @Query("""
        SELECT COUNT(r) FROM ResultadoAtividade r
        WHERE r.usuario.id = :usuarioId
          AND r.acertos = SIZE(r.atividade.perguntas)
        """)
    long contarAtividadesPerfeitas(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COALESCE(SUM(r.pontuacao), 0L) FROM ResultadoAtividade r WHERE r.usuario.id = :usuarioId")
    long somarPontuacaoDoUsuario(@Param("usuarioId") Long usuarioId);

    // Exportação dos dados: resultados da pessoa, já com a atividade
    @Query("""
        SELECT r FROM ResultadoAtividade r
        JOIN FETCH r.atividade
        WHERE r.usuario.id = :usuarioId
        ORDER BY r.id
        """)
    List<ResultadoAtividade> buscarDoUsuarioComAtividade(@Param("usuarioId") Long usuarioId);
}

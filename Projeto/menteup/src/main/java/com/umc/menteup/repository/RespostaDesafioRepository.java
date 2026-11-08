package com.umc.menteup.repository;

import com.umc.menteup.dto.TotalPorAluno;
import com.umc.menteup.model.RespostaDesafio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RespostaDesafioRepository extends JpaRepository<RespostaDesafio, Long> {

    long countByPergunta_Id(Long perguntaId);

    boolean existsByPergunta_IdAndUsuario_Id(Long perguntaId, Long usuarioId);

    Optional<RespostaDesafio> findByPergunta_IdAndUsuario_Id(Long perguntaId, Long usuarioId);

    @Query("""
            select r from RespostaDesafio r
            join fetch r.usuario
            join fetch r.pergunta p
            where p.desafio.id = :desafioId
            """)
    List<RespostaDesafio> buscarTodasDoDesafio(@Param("desafioId") Long desafioId);

    @Query("""
        select r from RespostaDesafio r
        join fetch r.pergunta p
        join fetch p.desafio d
        where r.usuario.id = :alunoId
        and d.turma.id = :turmaId
        order by d.id, p.ordem
        """)
    List<RespostaDesafio> buscarDoAlunoNaTurma(@Param("alunoId") Long alunoId, @Param("turmaId") Long turmaId);

    ///Pontos do aluno em desafios
    @Query("select coalesce(sum(r.pontos), 0L) from RespostaDesafio r where r.usuario.id = :usuarioId")
    long somarPontosDoUsuario(@Param("usuarioId") Long usuarioId);

    ///Pontos de desafios de cada aluno, usados no ranking
    @Query("""
            select new com.umc.menteup.dto.TotalPorAluno(r.usuario.id, sum(r.pontos), count(r.id))
            from RespostaDesafio r
            group by r.usuario.id
            """)
    List<TotalPorAluno> somarPorAluno();
}

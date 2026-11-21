package com.umc.menteup.repository;

import com.umc.menteup.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma,Long> {
    public List<Turma> findAllByNomeContainingIgnoreCase(String nome);

    List<Turma> findAllByAlunos_Id(Long alunoId);
    List<Turma> findAllByProfessor_Id(Long professorId);

    @Query("""
            select count(t)
            from Turma t
            join t.alunos a
            where t.id = :turmaId
            and a.id = :alunoId
            """)
    long contarMatricula(@Param("turmaId") Long turmaId, @Param("alunoId") Long alunoId);

    /// Turmas do mesmo professor com o mesmo nome (sem diferenciar maiúsculas), incluindo as arquivadas.
    /// professorId nulo: turmas sem professor responsável. ignorarId: a própria turma, ao renomear.
    @Query("""
            select count(t)
            from Turma t
            left join t.professor p
            where lower(trim(t.nome)) = lower(:nome)
            and ((:professorId is null and p is null) or p.id = :professorId)
            and (:ignorarId is null or t.id <> :ignorarId)
            """)
    long contarComMesmoNome(@Param("nome") String nome,
                            @Param("professorId") Long professorId,
                            @Param("ignorarId") Long ignorarId);
}

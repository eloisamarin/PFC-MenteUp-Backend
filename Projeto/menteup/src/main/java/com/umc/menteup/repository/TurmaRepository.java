package com.umc.menteup.repository;

import com.umc.menteup.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma,Long> {
    public List<Turma> findAllByNomeContainingIgnoreCase(String nome);

    List<Turma> findAllByAlunos_Id(Long alunoId);

    @Query("""
            select count(t)
            from Turma t
            join t.alunos a
            where t.id = :turmaId
            and a.id = :alunoId
            """)
    long contarMatricula(@Param("turmaId") Long turmaId, @Param("alunoId") Long alunoId);
}

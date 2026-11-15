package com.umc.menteup.repository;

import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.StatusAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {
    public List <Atividade> findAllByTituloContainingIgnoreCase(@Param("titulo") String titulo);
    long countByTurmas_IdAndStatusNot(Long turmaId, StatusAtividade status);

    List<Atividade> findAllByTurmas_Id(Long turmaId);

    ///Atividades de qualquer turma em que o aluno está, sem repetir as que estão em mais de uma turma dele
    @Query("""
            select a from Atividade a
                left join fetch a.usuario
            where exists (
                select 1 from Atividade a2 join a2.turmas t join t.alunos al
                where a2 = a and al.id = :alunoId
            )
                and a.status<> :statusExcluido
            order by a.dataCriacao desc
            """)
    List<Atividade> findVisiveisParaAluno(
            @Param("alunoId") Long alunoId,
            @Param("statusExcluido")StatusAtividade statusExluido
    );

    // Atividades que têm pelo menos uma turma do professor
    @Query("""
            select a from Atividade a
            where exists (
                select 1 from Atividade a2 join a2.turmas t
                where a2 = a and t.professor.id = :professorId
            )
            order by a.id
            """)
    List<Atividade> findDoProfessor(@Param("professorId") Long professorId);

    // Mesma lista, filtrada pelo título (sem diferenciar maiúsculas)
    @Query("""
            select a from Atividade a
            where exists (
                select 1 from Atividade a2 join a2.turmas t
                where a2 = a and t.professor.id = :professorId
            )
                and lower(a.titulo) like lower(concat('%', :titulo, '%'))
            order by a.id
            """)
    List<Atividade> findDoProfessorPorTitulo(
            @Param("professorId") Long professorId,
            @Param("titulo") String titulo
    );

}

package com.umc.menteup.repository;

import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.StatusAtividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {
    public List <Atividade> findAllByTituloContainingIgnoreCase(@Param("titulo") String titulo);

    @Query("""
            select a from Atividade a 
                join fetch a.turma t 
                left join fetch a.usuario
            where t.id in (
                select t2.id from Turma t2 join t2.alunos al where al.id = :alunoId
            )
                and a.status<> :statusExcluido
            order by a.dataCriacao desc
            """)
    List<Atividade> findVisiveisParaAluno(
            @Param("alunoId") Long alunoId,
            @Param("statusExcluido")StatusAtividade statusExluido
    );
}

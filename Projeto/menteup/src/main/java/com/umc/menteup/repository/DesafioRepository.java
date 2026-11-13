package com.umc.menteup.repository;

import com.umc.menteup.model.Desafio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DesafioRepository extends JpaRepository<Desafio, Long> {

    List<Desafio> findAllByOrderByDataCriacaoDesc();

    long countByTurma_Id(Long turmaId);

    @Query("""
            select d from Desafio d
            join fetch d.turma t
            where t.id in (
                select t2.id from Turma t2 join t2.alunos al where al.id = :alunoId
            )
            order by d.dataCriacao desc
            """)
    List<Desafio> findVisiveisParaAluno(@Param("alunoId") Long alunoId);
}

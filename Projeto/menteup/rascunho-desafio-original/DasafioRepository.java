package com.umc.menteup.repository;


import com.umc.menteup.model.Desafio;
import com.umc.menteup.model.StatusDesafio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DasafioRepository  extends JpaRepository <Desafio, Long {
    public List<Desafio> findAllByTituloContainingIgnoreCase(@Param("titulo" String titulo));

    @Query("""
            select a from Desafio d
                join fetch d.turma t
                left fetch d.usuario
            where t.id in (
            select t2.id from Turma t2 join t2.alunos al where al.id = :alunoId
            )
            and d.status<> : statusExluido
            order by d.dataCriacao desc

""")
    List<Desafio> findVisiveisParaAluno(
            @Param("alunoId") Long alunoId,
            @Param("statusExcluido")StatusDesafio statusExcluido
            );


}

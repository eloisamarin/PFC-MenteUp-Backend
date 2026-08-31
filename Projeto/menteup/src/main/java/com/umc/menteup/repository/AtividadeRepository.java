package com.umc.menteup.repository;

import com.umc.menteup.model.Atividade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {
    public List <Atividade> findAllByTituloContainingIgnoreCase(@Param("titulo") String titulo);
}

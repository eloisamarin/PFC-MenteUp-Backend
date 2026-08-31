package com.umc.menteup.repository;

import com.umc.menteup.model.Turma;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TurmaRepository extends JpaRepository<Turma,Long> {
    public List<Turma> findAllByNomeContainingIgnoreCase(String nome);

}

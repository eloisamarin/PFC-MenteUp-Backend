package com.umc.menteup.repository;

import com.umc.menteup.model.Conquista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConquistaRepository extends JpaRepository<Conquista, Long> {

    Optional<Conquista> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<Conquista> findAllByAtivaTrueOrderByOrdemExibicaoAsc();

    List<Conquista> findAllByOrderByOrdemExibicaoAsc();
}

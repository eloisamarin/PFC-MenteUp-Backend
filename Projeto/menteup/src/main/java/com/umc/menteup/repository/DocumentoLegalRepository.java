package com.umc.menteup.repository;

import com.umc.menteup.model.DocumentoLegal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentoLegalRepository extends JpaRepository<DocumentoLegal, Long> {

    // O tipo é gravado como texto (TERMO ou POLITICA)
    List<DocumentoLegal> findAllByTipo(String tipo);

    Optional<DocumentoLegal> findByTipoAndVersao(String tipo, String versao);
}

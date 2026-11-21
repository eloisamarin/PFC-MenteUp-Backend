package com.umc.menteup.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DocumentoLegalVersaoTest {

    @Test
    void proximaVersao_somaUmNaUltimaParte() {
        assertThat(DocumentoLegal.proximaVersao("1.1")).isEqualTo("1.2");
        assertThat(DocumentoLegal.proximaVersao("1.9")).isEqualTo("1.10");
        assertThat(DocumentoLegal.proximaVersao("2")).isEqualTo("2.1");
        assertThat(DocumentoLegal.proximaVersao(null)).isEqualTo("1.0");
    }

    @Test
    void normalizarConteudo_deixaOMesmoTextoNoWindowsENoLinux() {
        String windows = DocumentoLegal.normalizarConteudo("﻿# Termo\r\n\r\nTexto  \r\n");
        String linux = DocumentoLegal.normalizarConteudo("# Termo\n\nTexto\n");

        assertThat(windows).isEqualTo(linux);
        assertThat(DocumentoLegal.calcularHash(windows)).isEqualTo(DocumentoLegal.calcularHash(linux));
    }
}

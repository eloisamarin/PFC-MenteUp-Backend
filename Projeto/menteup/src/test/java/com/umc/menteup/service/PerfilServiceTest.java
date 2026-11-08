package com.umc.menteup.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerfilServiceTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "eloisa@gmail.com, el***@gmail.com",
            "ab@menteup.com, ab***@menteup.com",
            "a@x.com, a***@x.com",
            "sem-arroba, ***",
            "@sem-nome.com, ***"
    })
    void anonimizarEmail_deveEsconderOInicioEManterODominio(String email, String esperado) {
        assertEquals(esperado, PerfilService.anonimizarEmail(email));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void anonimizarEmail_comValorVazio_deveDevolverSoAsteriscos(String email) {
        assertEquals("***", PerfilService.anonimizarEmail(email));
    }
}

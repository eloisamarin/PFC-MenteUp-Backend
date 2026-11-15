package com.umc.menteup.model;

import java.util.Locale;
import java.util.Optional;

// Documentos que a pessoa precisa aceitar para usar a plataforma
// A pasta é onde ficam os textos de cada versão: src/main/resources/documentos/<pasta>/<versao>.md
public enum TipoDocumento {
    TERMO("termo"),
    POLITICA("politica");

    private final String pasta;

    TipoDocumento(String pasta) {
        this.pasta = pasta;
    }

    public String getPasta() {
        return pasta;
    }

    // Aceita "termo", "TERMO", "politica"... e devolve vazio para qualquer outro valor
    public static Optional<TipoDocumento> doTexto(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String procurado = valor.trim().toLowerCase(Locale.ROOT);
        for (TipoDocumento tipo : values()) {
            if (tipo.pasta.equals(procurado) || tipo.name().toLowerCase(Locale.ROOT).equals(procurado)) {
                return Optional.of(tipo);
            }
        }
        return Optional.empty();
    }
}

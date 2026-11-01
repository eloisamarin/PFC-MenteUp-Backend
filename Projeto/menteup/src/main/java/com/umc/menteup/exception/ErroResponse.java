package com.umc.menteup.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/**
 * Formato único de erro da API.
 * "campos" só aparece em erros de validação (campo -> mensagem).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(
        String codigo,
        String mensagem,
        Map<String, String> campos
) {
}

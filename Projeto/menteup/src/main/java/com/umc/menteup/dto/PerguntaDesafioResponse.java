package com.umc.menteup.dto;

import java.util.List;

public record PerguntaDesafioResponse(
        Long id,
        String enunciado,
        Integer tempoSegundos,
        List<AlternativaResponse> alternativas
) {
}

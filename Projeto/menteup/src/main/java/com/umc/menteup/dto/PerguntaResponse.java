package com.umc.menteup.dto;

import java.util.List;

public record PerguntaResponse(
        Long id,
        String texto,
        List<AlternativaResponse> alternativas
) {
}

package com.umc.menteup.dto;

import java.util.List;

public record RespostaProgressoResponse(
        Long perguntaId,
        Long alternativaId,
        List<Long> alternativasIds
) {
    public RespostaProgressoResponse(Long perguntaId, Long alternativaId) {
        this(perguntaId, alternativaId, null);
    }
}

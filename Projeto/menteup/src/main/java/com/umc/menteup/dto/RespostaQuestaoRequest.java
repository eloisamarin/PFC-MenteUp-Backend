package com.umc.menteup.dto;

import jakarta.validation.constraints.NotNull;

public record RespostaQuestaoRequest(
        @NotNull(message = "perguntaId é obrigatório")
        Long perguntaId,

        @NotNull(message = "alternativaId é obrigatório")
        Long alternativaId
) {
}

package com.umc.menteup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ConclusaoAtividadeRequest(
        @NotNull(message = "As respostas são obrigatórios")
        List<@Valid @NotNull RespostaQuestaoRequest> respostas
) {
}

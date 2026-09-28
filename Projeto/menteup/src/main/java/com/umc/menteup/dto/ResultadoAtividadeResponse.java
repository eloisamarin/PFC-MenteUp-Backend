package com.umc.menteup.dto;

public record ResultadoAtividadeResponse(
        Long atividadeId,
        String titulo,
        Integer totalQuestoes,
        Integer acertos,
        Integer pontuacao
) {
}

package com.umc.menteup.dto;

import java.util.List;

public record ProgressoAtividadeResponse(
        Integer percentualConclusao,
        Integer respondidas,
        Integer totalQuestoes,
        List<RespostaProgressoResponse> respostas
) {
}

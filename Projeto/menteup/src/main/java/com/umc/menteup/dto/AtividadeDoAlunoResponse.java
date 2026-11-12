package com.umc.menteup.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AtividadeDoAlunoResponse(
        Long resultadoId,
        Long atividadeId,
        String titulo,
        String tipo,
        Integer pontuacao,
        Integer pontuacaoMaxima,
        Integer acertos,
        Integer totalQuestoes,
        LocalDateTime concluidaEm,
        String comentario,
        String comentadoPor,
        List<QuestaoAlunoResponse> questoes
) {
}

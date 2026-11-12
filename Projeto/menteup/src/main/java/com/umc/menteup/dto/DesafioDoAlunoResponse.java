package com.umc.menteup.dto;

import java.util.List;

public record DesafioDoAlunoResponse(
        Long desafioId,
        String titulo,
        Integer pontos,
        Integer acertos,
        Integer totalPerguntas,
        List<QuestaoAlunoResponse> questoes
) {
}

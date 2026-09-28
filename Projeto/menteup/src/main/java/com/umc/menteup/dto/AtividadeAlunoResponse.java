package com.umc.menteup.dto;

import com.umc.menteup.model.StatusAlunoAtividade;

public record AtividadeAlunoResponse(
        AtividadeResponse atividade,
        StatusAlunoAtividade status,
        Integer acertos,
        Integer pontuacao,
        Integer percentualConclusao
) {
}

package com.umc.menteup.dto;

import com.umc.menteup.model.StatusAlunoAtividade;

public record AtividadeAlunoResponse(
        AtividadeResponse atividade,
        StatusAlunoAtividade status,
        Integer acertos,
        Integer pontuacao,
        Integer percentualConclusao,
        /// Comentário do professor no resultado (nulo enquanto não houver)
        String comentarioProfessor
) {
}

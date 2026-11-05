package com.umc.menteup.dto;

import java.util.List;

public record DesempenhoTurmaResponse(
        Long turmaId, String nomeTurma, Integer totalAlunos, Long totalAtividades,
        Double mediaPontuacao, Integer taxaParticipacao,
        List<DesempenhoAlunoResponse> alunos
) {
}

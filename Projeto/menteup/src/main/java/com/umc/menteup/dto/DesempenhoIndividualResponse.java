package com.umc.menteup.dto;

import java.util.List;

public record DesempenhoIndividualResponse(
        Long alunoId,
        String nomeAluno,
        Long turmaId,
        String nomeTurma,
        List<AtividadeDoAlunoResponse> atividades,
        List<DesafioDoAlunoResponse> desafios


) {
}

package com.umc.menteup.dto;

public record DesempenhoAlunoResponse(
        Long alunoId,
        String nomeAluno,
        Long pontuacao,
        Long acertos,
        Long atividadesConcluidas
) {
}

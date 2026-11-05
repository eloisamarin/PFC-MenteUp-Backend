package com.umc.menteup.dto;

public  record ResumoDesempenhoAluno(
        Long alunoId,
        Long pontuacao,
        Long acertos,
        Long atividadesConcluidas
) {
}


package com.umc.menteup.dto;

///Soma de pontos e quantidade de registros de um aluno, usada para montar o ranking
public record TotalPorAluno(
        Long usuarioId,
        Long pontos,
        Long quantidade
) {
}

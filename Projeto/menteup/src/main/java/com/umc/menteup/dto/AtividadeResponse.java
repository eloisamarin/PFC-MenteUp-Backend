package com.umc.menteup.dto;

public record AtividadeResponse(
        Long id,
        String titulo,
        String descricao,
        TurmaResponse turma,
        UsuarioResponse usuario
){
}

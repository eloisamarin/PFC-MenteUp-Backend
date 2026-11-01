package com.umc.menteup.dto;

import com.umc.menteup.model.StatusAtividade;

import java.time.LocalDateTime;

public record AtividadeResponse(
        Long id,
        String titulo,
        String descricao,
        TurmaResponse turma,
        Integer pontuacao,
        StatusAtividade status,
        UsuarioResponse usuario,
        LocalDateTime dataCriacao
){
}

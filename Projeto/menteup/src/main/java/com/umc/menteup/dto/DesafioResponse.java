package com.umc.menteup.dto;

import com.umc.menteup.model.StatusDesafio;

import java.time.LocalDateTime;

public record DesafioResponse(
        Long id,
        String titulo,
        String descricao,
        StatusDesafio status,
        TurmaResponse turma,
        Integer totalPerguntas,
        LocalDateTime dataCriacao
) {
}

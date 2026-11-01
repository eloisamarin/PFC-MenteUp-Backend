package com.umc.menteup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CriarDesafioRequest(
        @NotBlank(message = "O atributo título é obrigatório!")
        @Size(min = 5, max = 100, message = "O atributo título deve ter no mínimo 5 e no máximo 100 caracteres")
        String titulo,

        @NotBlank(message = "O atributo Descrição é obrigatório!")
        @Size(min = 10, max = 1000, message = "O atributo Descrição deve ter no mínimo 10 e no máximo 1000 caracteres")
        String descricao,

        @NotNull(message = "A turma do desafio é obrigatória")
        Long turmaId,

        @NotEmpty(message = "O desafio precisa ter ao menos uma pergunta")
        List<@Valid @NotNull PerguntaDesafioRequest> perguntas
) {
}

package com.umc.menteup.dto;

import com.umc.menteup.model.TipoAtividade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuestionarioRequest(
        /** Template escolhido na etapa 2. Opcional: se vier nulo, mantém o tipo atual da atividade (QUIZ por padrão). */
        TipoAtividade tipo,

        @NotEmpty
        List<@Valid PerguntaRequest> perguntas
){
}

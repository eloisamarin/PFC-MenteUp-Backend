package com.umc.menteup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record QuestionarioRequest(
        @NotEmpty
        List<@Valid PerguntaRequest> perguntas
){
}

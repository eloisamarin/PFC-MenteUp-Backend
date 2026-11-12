package com.umc.menteup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PerguntaRequest(
        @NotBlank(message = "O enunciado é obrigatório.")
        @Size(max = 255, message = "O enunciado deve ter no máximo 255 caracteres.")
        String enunciado,

        @NotEmpty
        List<@Valid AlternativaRequest> alternativas
){
}

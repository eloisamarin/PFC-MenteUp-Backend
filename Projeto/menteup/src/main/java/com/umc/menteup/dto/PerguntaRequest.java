package com.umc.menteup.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record PerguntaRequest(
        @NotBlank
        String enunciado,

        @NotEmpty
        List<@Valid AlternativaRequest> alternativas
){
}

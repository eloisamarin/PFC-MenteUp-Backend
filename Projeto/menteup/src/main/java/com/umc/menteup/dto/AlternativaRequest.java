package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AlternativaRequest (
        @NotBlank(message = "O texto da alternativa é obrigatório.")
        @Size(max = 255, message = "A alternativa deve ter no máximo 255 caracteres.")
        String texto,

        @NotNull
        Boolean correta,

        Integer ordem
) {
    @JsonCreator
    public AlternativaRequest {
    }

    public AlternativaRequest(String texto, Boolean correta) {
        this(texto, correta, null);
    }
}

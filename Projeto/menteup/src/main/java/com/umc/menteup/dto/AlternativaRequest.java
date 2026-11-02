package com.umc.menteup.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlternativaRequest (
        @NotBlank
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

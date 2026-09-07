package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AlternativaRequest (
        @NotBlank
        String texto,

        @NotNull
        Boolean correta
) {
}

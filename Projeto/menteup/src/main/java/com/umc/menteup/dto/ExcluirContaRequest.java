package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;

public record ExcluirContaRequest(
        @NotBlank(message = "Informe a sua senha para confirmar")
        String senha
) {
}

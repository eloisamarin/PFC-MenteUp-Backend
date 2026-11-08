package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record RedefinirSenhaRequest(
        @NotBlank(message = "O link de redefinição é obrigatório")
        String token,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, message = "A nova senha deve ter no mínimo 8 caracteres")
        String novaSenha
) {
}

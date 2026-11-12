package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;


public record RedefinirSenhaRequest(
        @NotBlank(message = "O link de redefinição é obrigatório")
        String token,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres")
        String novaSenha
) {
}

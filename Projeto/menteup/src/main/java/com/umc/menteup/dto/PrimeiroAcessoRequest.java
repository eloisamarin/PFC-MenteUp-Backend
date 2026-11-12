package com.umc.menteup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// POST /usuarios/senha/primeiro-acesso: link do convite + senha nova + aceite do termo
public record PrimeiroAcessoRequest(
        @NotBlank(message = "O link de primeiro acesso é obrigatório")
        String token,

        @NotBlank(message = "Informe a nova senha")
        @Size(min = 8, max = 72, message = "A nova senha deve ter de 8 a 72 caracteres")
        String novaSenha,

        // Precisa ser true: quem não aceita o termo não usa a plataforma
        boolean aceitouTermos
) {
}

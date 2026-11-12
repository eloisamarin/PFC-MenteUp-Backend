package com.umc.menteup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** POST /usuarios/reenviar-2fa: só o e-mail de quem acabou de entrar com a senha. */
public record ReenviarCodigo2FARequest(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String usuario
) {
}

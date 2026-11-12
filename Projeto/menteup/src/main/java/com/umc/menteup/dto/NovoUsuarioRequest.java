package com.umc.menteup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// POST /usuarios (só administrador): sem senha, porque a pessoa cria a própria senha pelo convite
public record NovoUsuarioRequest(
        @NotBlank(message = "Informe o nome")
        String nomeUsuario,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "Informe um e-mail válido")
        String usuario,

        @NotBlank(message = "Informe o tipo de usuário")
        String tipoUsuario
) {
}

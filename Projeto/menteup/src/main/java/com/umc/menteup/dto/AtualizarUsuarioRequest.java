package com.umc.menteup.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/*
 * PUT /usuarios/atualizar: só os dados que podem ser editados ali.
 * A senha NÃO vem aqui (troca de senha é em PUT /usuarios/me/senha, pedindo a senha atual),
 * e o token do celular, o avatar e os dados de login ficam como estão no banco.
 * O id é conferido no service (erro ID_USUARIO_OBRIGATORIO).
 */
public record AtualizarUsuarioRequest(
        Long id,

        @NotBlank(message = "O nome é obrigatório.")
        @Size(max = 100, message = "O nome deve ter no máximo 100 caracteres.")
        String nomeUsuario,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        String usuario,

        @NotBlank(message = "O tipo de usuário é obrigatório.")
        String tipoUsuario
) {
}

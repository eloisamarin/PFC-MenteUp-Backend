package com.umc.menteup.dto;

/// Aluno com o desafio aberto (sala de espera). Só id, nome e avatar: nada de e-mail (LGPD)
public record AlunoNaSalaResponse(
        Long id,
        String nome,
        String avatar
) {
}

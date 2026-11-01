package com.umc.menteup.dto;

import java.util.List;

// Mesmo formato do corpo do PUT e com o gabarito: exclusivo de professor e administrador
public record DesafioEdicaoResponse(
        Long id,
        String titulo,
        String descricao,
        Long turmaId,
        List<PerguntaDesafioRequest> perguntas
) {
}

package com.umc.menteup.dto;

import java.time.LocalDateTime;

public record ComentarioResponse(
        Long resultadoId,
        String comentario,
        String comentadoPor,
        LocalDateTime comentadoEm
) {
}

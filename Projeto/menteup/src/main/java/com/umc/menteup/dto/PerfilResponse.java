package com.umc.menteup.dto;

import java.util.List;

public record PerfilResponse(
        Long id,
        String nome,
        String emailAnonimizado,
        String tipoUsuario,
        String avatar,
        List<TurmaResponse> turmas
) {
}


package com.umc.menteup.dto;

public record RankingResponse(
        Long usuarioId,
        String nomeUsuario,
        Long pontuacaoTotal,
        Long atividadeConcluidas
) {
}

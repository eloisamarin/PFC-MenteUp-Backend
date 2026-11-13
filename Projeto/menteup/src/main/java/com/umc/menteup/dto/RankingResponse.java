package com.umc.menteup.dto;

public record RankingResponse(
        Long usuarioId,
        String nomeUsuario,
        Long pontuacaoTotal,
        Long atividadeConcluidas,
        // Animal escolhido no perfil (ex.: "PANDA"); null mostra as iniciais
        String avatar
) {
}

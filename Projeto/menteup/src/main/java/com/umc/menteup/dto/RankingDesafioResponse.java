package com.umc.menteup.dto;

public record RankingDesafioResponse(
        Integer posicao,
        Long usuarioId,
        String nomeUsuario,
        Integer pontos,
        Integer acertos,
        // Animal escolhido no perfil (ex.: "PANDA"); null mostra as iniciais
        String avatar
) {
}

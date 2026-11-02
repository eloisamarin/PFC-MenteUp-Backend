package com.umc.menteup.dto;

import com.umc.menteup.model.StatusDesafio;

/**
 * Foto do jogo neste instante; o front consulta este estado a cada segundo.
 * "pergunta" só vem preenchida com o desafio em andamento, e o gabarito
 * (alternativaCorretaId, meusPontos) só depois que a pergunta encerra.
 */
public record EstadoDesafioResponse(
        Long desafioId,
        String titulo,
        StatusDesafio status,
        Integer numeroPergunta,
        Integer totalPerguntas,
        PerguntaDesafioResponse pergunta,
        Integer segundosRestantes,
        Boolean perguntaEncerrada,
        Long respostasRecebidas,
        Long minhaAlternativaId,
        Long alternativaCorretaId,
        Integer meusPontos
) {
}

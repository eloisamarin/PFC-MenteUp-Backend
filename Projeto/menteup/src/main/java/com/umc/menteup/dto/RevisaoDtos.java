package com.umc.menteup.dto;

import java.util.List;

import com.umc.menteup.model.TipoAtividade;

/// Revisão das perguntas depois que o aluno responde (atividade e desafio ao vivo)
public final class RevisaoDtos {

    private RevisaoDtos() {
    }

    /// Revisão da atividade enviada.
    /// "respostaCorreta" só vem preenchida quando o professor encerra a atividade (gabaritoLiberado),
    /// para o gabarito não circular entre os alunos que ainda estão respondendo.
    public record RevisaoAtividade(
            Long atividadeId,
            String titulo,
            TipoAtividade tipo,
            boolean gabaritoLiberado,
            List<QuestaoAtividade> questoes
    ) {
    }

    /// Uma pergunta da atividade: o que o aluno respondeu, se acertou e (depois de encerrada) a resposta certa.
    /// As respostas são listas porque Ordenação e Lacunas têm vários itens, na ordem.
    public record QuestaoAtividade(
            Long perguntaId,
            String enunciado,
            List<String> suaResposta,
            Boolean acertou,
            List<String> respostaCorreta
    ) {
    }

    /// Revisão do desafio ao vivo (só depois de encerrado, quando todos já responderam)
    public record RevisaoDesafio(
            Long desafioId,
            String titulo,
            int pontosTotais,
            List<QuestaoDesafio> questoes
    ) {
    }

    /// Uma pergunta do desafio: a alternativa do aluno (nula se não respondeu a tempo), os pontos e a correta
    public record QuestaoDesafio(
            Long perguntaId,
            String enunciado,
            String suaResposta,
            boolean acertou,
            int pontos,
            String respostaCorreta
    ) {
    }
}

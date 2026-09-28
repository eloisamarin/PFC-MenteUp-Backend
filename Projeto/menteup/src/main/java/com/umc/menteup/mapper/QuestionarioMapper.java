package com.umc.menteup.mapper;

import com.umc.menteup.dto.*;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.ResultadoAtividade;

import java.util.List;

/**
 * Conversões entidade -> DTO compartilhadas pelos controllers e services,
 * para que todas as telas enxerguem exatamente o mesmo formato.
 */
public final class QuestionarioMapper {

    private QuestionarioMapper() {
    }

    public static AtividadeResponse toAtividadeResponse(Atividade atividade) {
        return new AtividadeResponse(
                atividade.getId(),
                atividade.getTitulo(),
                atividade.getDescricao(),
                atividade.getTurma() == null
                        ? null
                        : new TurmaResponse(atividade.getTurma().getId(), atividade.getTurma().getNome()),
                atividade.getPontuacao(),
                atividade.getStatus(),
                atividade.getUsuario() == null
                        ? null
                        : new UsuarioResponse(atividade.getUsuario().getId(), atividade.getUsuario().getNomeUsuario()),
                atividade.getDataCriacao()
        );
    }

    public static List<PerguntaResponse> toPerguntas(Atividade atividade) {
        return atividade.getPerguntas().stream()
                .map(pergunta -> new PerguntaResponse(
                        pergunta.getId(),
                        pergunta.getEnunciado(),
                        pergunta.getAlternativas().stream()
                                .map(alternativa -> new AlternativaResponse(alternativa.getId(), alternativa.getTexto()))
                                .toList()
                ))
                .toList();
    }

    public static ProgressoAtividadeResponse toProgresso(
            com.umc.menteup.model.ProgressoAtividade progresso,
            int totalQuestoes
    ) {
        if (progresso == null) {
            return null;
        }
        int respondidas = progresso.getRespostas().size();
        int percentual = totalQuestoes == 0
                ? 0
                : (int) Math.round(
                (respondidas * 100.0) / totalQuestoes);
        List<RespostaProgressoResponse> respostas =
                progresso.getRespostas()
                        .stream()
                        .map(resposta ->
                                new RespostaProgressoResponse(
                                        resposta.getPergunta().getId(),
                                        resposta.getAlternativa().getId()
                                )
                        )
                        .toList();
        return new ProgressoAtividadeResponse(
                percentual,
                respondidas,
                totalQuestoes,
                respostas
        );
    }

    public static QuestionarioResponse toQuestionario(
            Atividade atividade,
            ResultadoAtividadeResponse meuResultado,
            ProgressoAtividadeResponse meuProgresso
    ) {
        return new QuestionarioResponse(
                toAtividadeResponse(atividade),
                toPerguntas(atividade),
                meuResultado,
                meuProgresso
        );
    }

    public static ResultadoAtividadeResponse toResultado(
            ResultadoAtividade resultado,
            int totalQuestoes
    ) {
        return new ResultadoAtividadeResponse(
                resultado.getAtividade().getId(),
                resultado.getAtividade().getTitulo(),
                totalQuestoes,
                resultado.getAcertos(),
                resultado.getPontuacao()
        );
    }
}

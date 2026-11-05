package com.umc.menteup.mapper;

import com.umc.menteup.dto.*;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.ResultadoAtividade;

import java.util.List;

///Converte as DTO's compartilhadas pelos controllers e services para que todas as telas vejam exatamente o mesmo formato
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
                atividade.getDataCriacao(),
                atividade.getTipo()
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

    public static List<PerguntaResponse> toPerguntasComGabarito(Atividade atividade) {
        return atividade.getPerguntas().stream()
                .map(pergunta -> new PerguntaResponse(
                        pergunta.getId(),
                        pergunta.getEnunciado(),
                        pergunta.getAlternativas().stream()
                                .map(alternativa -> new AlternativaResponse(
                                        alternativa.getId(),
                                        alternativa.getTexto(),
                                        alternativa.getCorreta(),
                                        alternativa.getOrdem()
                                ))
                                .toList()
                ))
                .toList();
    }

    public static QuestionarioResponse toQuestionarioProfessor(Atividade atividade) {
        return toQuestionario(atividade, toPerguntasComGabarito(atividade), null, null);
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
                        .map(QuestionarioMapper::toRespostaProgresso)
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
        return toQuestionario(atividade, toPerguntas(atividade), meuResultado, meuProgresso);
    }

    public static QuestionarioResponse toQuestionario(
            Atividade atividade,
            List<PerguntaResponse> perguntas,
            ResultadoAtividadeResponse meuResultado,
            ProgressoAtividadeResponse meuProgresso
    ) {
        return new QuestionarioResponse(
                toAtividadeResponse(atividade),
                perguntas,
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

    public static RespostaProgressoResponse toRespostaProgresso(com.umc.menteup.model.RespostaAtividade resposta) {
        return new RespostaProgressoResponse(
                resposta.getPergunta().getId(),
                resposta.getAlternativa() == null ? null : resposta.getAlternativa().getId(),
                resposta.getAlternativasIds().isEmpty() ? null : List.copyOf(resposta.getAlternativasIds())
        );
    }
}



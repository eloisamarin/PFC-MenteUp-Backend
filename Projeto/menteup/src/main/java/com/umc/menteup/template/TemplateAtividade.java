package com.umc.menteup.template;

import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.PerguntaResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;
import com.umc.menteup.model.TipoAtividade;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface TemplateAtividade {

    TipoAtividade tipo();

    default void validarCriacao(PerguntaRequest pergunta) {
    }

    ///Irá validar o questionário
    default void validarQuestionario(Atividade atividade, List<PerguntaRequest> perguntas) {
        perguntas.forEach(this::validarCriacao);
    }

    ///Estrutura as perguntas
    default List<PerguntaResponse> perguntasParaAluno(Atividade atividade) {
        return QuestionarioMapper.toPerguntas(atividade);
    }

    void validarResposta(Pergunta pergunta, SalvarRespostaRequest request);

    void preencherResposta(RespostaAtividade resposta, Pergunta pergunta, SalvarRespostaRequest request);

    boolean estaCorreta(Pergunta pergunta, RespostaAtividade resposta);

    /// Revisão: o texto do que o aluno respondeu, na ordem em que respondeu.
    /// Quiz, Verdadeiro ou Falso e Associação guardam uma alternativa; Ordenação e Lacunas, uma sequência.
    default List<String> respostaDoAluno(Pergunta pergunta, RespostaAtividade resposta) {
        if (resposta.getAlternativa() != null) {
            return List.of(resposta.getAlternativa().getTexto());
        }
        Map<Long, String> textoPorId = pergunta.getAlternativas().stream()
                .collect(Collectors.toMap(Alternativa::getId, Alternativa::getTexto));
        return resposta.getAlternativasIds().stream()
                .map(id -> textoPorId.getOrDefault(id, "?"))
                .toList();
    }

    /// Revisão: a resposta certa. Por padrão, as alternativas corretas na ordem cadastrada
    /// (uma no Quiz, no Verdadeiro ou Falso e na Associação; uma por lacuna nas Lacunas)
    default List<String> respostaCorreta(Pergunta pergunta) {
        return pergunta.getAlternativas().stream()
                .filter(alternativa -> Boolean.TRUE.equals(alternativa.getCorreta()))
                .sorted(Comparator.comparing(Alternativa::getOrdem, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(Alternativa::getTexto)
                .toList();
    }
}

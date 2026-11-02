package com.umc.menteup.template;

import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.PerguntaResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.mapper.QuestionarioMapper;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;
import com.umc.menteup.model.TipoAtividade;

import java.util.List;

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
}

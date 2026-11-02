package com.umc.menteup.template;

import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;

@Component
public class

QuizTemplate implements TemplateAtividade {

    @Override
    public TipoAtividade tipo() {
        return TipoAtividade.QUIZ;
    }

    @Override
    public void validarResposta(Pergunta pergunta, SalvarRespostaRequest request) {
        buscarAlternativa(pergunta, request.alternativaId(), "A alternativa não pertence a esta perguta.");
    }

    @Override
    public void preencherResposta(RespostaAtividade resposta, Pergunta pergunta, SalvarRespostaRequest request) {
        resposta.setAlternativa(
                buscarAlternativa(pergunta, request.alternativaId(), "A alternativa não pertence a esta perguta.")
        );
    }

    @Override
    public boolean estaCorreta(Pergunta pergunta, RespostaAtividade resposta) {
        Long alternativaId = resposta.getAlternativa().getId();

        Alternativa alternativa = buscarAlternativa(
                pergunta,
                alternativaId,
                "A alternativa" + alternativaId + "não pertence à pergunta" + pergunta.getId()
        );

        return Boolean.TRUE.equals(alternativa.getCorreta());
    }

    ///Vai procurar a alternativa dentro da própria questão
    protected Alternativa buscarAlternativa(Pergunta pergunta, Long alternativaId, String mensagemErro) {
        return pergunta.getAlternativas()
                .stream()
                .filter(a -> a.getId().equals(alternativaId))
                .findFirst()
                .orElseThrow(() -> ApiException.requisicaoInvalida("ALTERNATIVA_INVALIDA", mensagemErro));
    }
}

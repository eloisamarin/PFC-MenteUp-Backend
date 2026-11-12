package com.umc.menteup.template;

import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;
/// A lógic de resposta e correção são herdadas do QuizTemplate
@Component
public class VerdadeiroFalsoTemplate extends QuizTemplate {

    @Override
    public TipoAtividade tipo() {
        return TipoAtividade.VERDADEIRO_FALSO;
    }

    ///2 alternativas e 1 correta
    @Override
    public void validarCriacao(PerguntaRequest pergunta) {
        if (pergunta.alternativas().size() != 2) {
            throw ApiException.requisicaoInvalida(
                    "VERDADEIRO_FALSO_INVALIDO",
                    "A afirmação \"" + pergunta.enunciado()
                            + "\" deve ter exatamente 2 alternativas (Verdadeiro e Falso)."
            );
        }

        long corretas = pergunta.alternativas()
                .stream()
                .filter(alternativa -> Boolean.TRUE.equals(alternativa.correta()))
                .count();

        if (corretas != 1) {
            throw ApiException.requisicaoInvalida(
                    "VERDADEIRO_FALSO_INVALIDO",
                    "A afirmação \"" + pergunta.enunciado() + "\" deve ter apenas uma alternativa correta."
            );
        }
    }
}

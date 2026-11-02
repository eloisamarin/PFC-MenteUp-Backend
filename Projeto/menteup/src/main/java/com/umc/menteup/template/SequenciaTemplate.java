package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaResponse;
import com.umc.menteup.dto.PerguntaResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

///Base de templates das atividades de ordenação de frases e preenchimento de lacunas
public abstract class SequenciaTemplate implements TemplateAtividade {

    ///Quantidade de respostas que o aluno enviar nesta pergunta
    protected abstract int tamanhoEsperado(Pergunta pergunta);

    ///Diz a sequencia correta das respostas
    protected abstract boolean sequenciaCorreta(Pergunta pergunta, List<Alternativa> sequencia);

    /// Embaralha as alternativas para não revelar a ordem correta
    @Override
    public List<PerguntaResponse> perguntasParaAluno(Atividade atividade) {
        return atividade.getPerguntas().stream()
                .map(p -> new PerguntaResponse(p.getId(), p.getEnunciado(), embaralhar(p)))
                .toList();
    }

    @Override
    public void validarResposta(Pergunta pergunta, SalvarRespostaRequest request) {
        buscarSequencia(pergunta, request.alternativasIds());
    }

    @Override
    public void preencherResposta(RespostaAtividade resposta, Pergunta pergunta, SalvarRespostaRequest request) {
        buscarSequencia(pergunta, request.alternativasIds());
        resposta.setAlternativa(null);
        resposta.getAlternativasIds().clear();
        resposta.getAlternativasIds().addAll(request.alternativasIds());
    }

    @Override
    public boolean estaCorreta(Pergunta pergunta, RespostaAtividade resposta) {
        return sequenciaCorreta(pergunta, buscarSequencia(pergunta, resposta.getAlternativasIds()));
    }

    ///Conferência de tamanho e repetição
    protected List<Alternativa> buscarSequencia(Pergunta pergunta, List<Long> alternativasIds) {
        int esperado = tamanhoEsperado(pergunta);

        if (alternativasIds == null || alternativasIds.size() != esperado) {
            throw ApiException.requisicaoInvalida(
                    "SEQUENCIA_INVALIDA",
                    "A resposta da pergunta " + pergunta.getId() + " deve ter " + esperado + " itens."
            );
        }
        if (new HashSet<>(alternativasIds).size() != alternativasIds.size()) {
            throw ApiException.requisicaoInvalida(
                    "SEQUENCIA_INVALIDA",
                    "Um item não pode ser usado mais de uma vez na mesma resposta."
            );
        }

        return alternativasIds.stream()
                .map(id -> pergunta.getAlternativas().stream()
                        .filter(a -> a.getId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> ApiException.requisicaoInvalida(
                                "ALTERNATIVA_INVALIDA",
                                "A alternativa " + id + " não pertence à pergunta " + pergunta.getId() + "."
                        )))
                .toList();
    }

    ///Embaralha as alternativas com base no id da pergunta, mantendo a ordem mesmo quando recarrega a página
    private List<AlternativaResponse> embaralhar(Pergunta pergunta) {
        List<AlternativaResponse> cadastradas = pergunta.getAlternativas().stream()
                .map(a -> new AlternativaResponse(a.getId(), a.getTexto()))
                .toList();

        List<AlternativaResponse> embaralhadas = new ArrayList<>(cadastradas);
        Collections.shuffle(embaralhadas, new Random(pergunta.getId() == null ? 0L : pergunta.getId()));

        ///Se o embaralhamento cair na mesma ordem do cadastro, desloca uma posição
        if (embaralhadas.size() > 1 && embaralhadas.equals(cadastradas)) {
            Collections.rotate(embaralhadas, 1);
        }
        return List.copyOf(embaralhadas);
    }
}

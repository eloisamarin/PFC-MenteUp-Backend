package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaResponse;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.PerguntaResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;
import com.umc.menteup.model.TipoAtividade;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
///Permite que o aluno ligue o item da coluna A a repostas da coluna B.
@Component
public class AssociacaoTemplate implements TemplateAtividade {

    @Override
    public TipoAtividade tipo() {
        return TipoAtividade.ASSOCIACAO;
    }

    ///Um item da coluna A possui apenas 1 par que estará marcado como correto
    @Override
    public void validarCriacao(PerguntaRequest pergunta) {
        if (pergunta.alternativas().size() != 1
                || !Boolean.TRUE.equals(pergunta.alternativas().get(0).correta())) {
            throw invalida("O item \"" + pergunta.enunciado() + "\" deve ter exatamente 1 par correto.");
        }
    }

    ///A atividade deve possuir no mínimo 2 pares e sem ter respostas repetidas na coluna B
    @Override
    public void validarQuestionario(Atividade atividade, List<PerguntaRequest> perguntas) {
        perguntas.forEach(this::validarCriacao);

        if (atividade.getPerguntas().size() + perguntas.size() < 2) {
            throw invalida("A associação de colunas precisa de pelo menos 2 pares.");
        }

        Set<String> textosColunaB = new HashSet<>();
        atividade.getPerguntas().forEach(p ->
                p.getAlternativas().forEach(a -> textosColunaB.add(normalizar(a.getTexto()))));

        for (PerguntaRequest pergunta : perguntas) {
            String texto = pergunta.alternativas().get(0).texto();
            if (!textosColunaB.add(normalizar(texto))) {
                throw invalida("O texto \"" + texto + "\" aparece mais de uma vez na coluna B.");
            }
        }
    }

    @Override
    public List<PerguntaResponse> perguntasParaAluno(Atividade atividade) {
        List<AlternativaResponse> colunaB = new ArrayList<>(
                colunaB(atividade).stream()
                        .map(a -> new AlternativaResponse(a.getId(), a.getTexto()))
                        .toList()
        );
        ///Ordem da coluna B fixa calculada pelo id da atividade, fazendo com que a rdem não mude quando recarregar a página
        Collections.shuffle(colunaB, new Random(atividade.getId() == null ? 0L : atividade.getId()));

        List<AlternativaResponse> colunaFinal = List.copyOf(colunaB);

        return atividade.getPerguntas().stream()
                .map(p -> new PerguntaResponse(p.getId(), p.getEnunciado(), colunaFinal))
                .toList();
    }

    @Override
    public void validarResposta(Pergunta pergunta, SalvarRespostaRequest request) {
        buscarNaColunaB(pergunta, request.alternativaId());
    }

    @Override
    public void preencherResposta(RespostaAtividade resposta, Pergunta pergunta, SalvarRespostaRequest request) {
        resposta.setAlternativa(buscarNaColunaB(pergunta, request.alternativaId()));
    }

    ///Consta como correta quando o item selecionado na coluna B é o par cadastrado para esta pergunta
    @Override
    public boolean estaCorreta(Pergunta pergunta, RespostaAtividade resposta) {
        Alternativa escolhida = buscarNaColunaB(pergunta, resposta.getAlternativa().getId());

        return pergunta.getAlternativas()
                .stream()
                .anyMatch(a -> a.getId().equals(escolhida.getId()));
    }

    ///Aglomera as alternativas das perguntas da atividade
    private List<Alternativa> colunaB(Atividade atividade) {
        return atividade.getPerguntas().stream()
                .flatMap(p -> p.getAlternativas().stream())
                .toList();
    }

    ///A alternativa pode ser de outra pergunta, desde que seja da mesma atividade
    private Alternativa buscarNaColunaB(Pergunta pergunta, Long alternativaId) {
        return colunaB(pergunta.getAtividade())
                .stream()
                .filter(a -> a.getId().equals(alternativaId))
                .findFirst()
                .orElseThrow(() -> ApiException.requisicaoInvalida(
                        "ALTERNATIVA_INVALIDA",
                        "A alternativa " + alternativaId + " não pertence a esta atividade."
                ));
    }

    private static String normalizar(String texto) {
        return texto == null ? "" : texto.trim().toLowerCase();
    }

    private static ApiException invalida(String mensagem) {
        return ApiException.requisicaoInvalida("ASSOCIACAO_INVALIDA", mensagem);
    }
}

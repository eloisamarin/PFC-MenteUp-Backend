package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaRequest;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AssociacaoTemplateTest {

    private final AssociacaoTemplate template = new AssociacaoTemplate();

    private Atividade atividade;
    private Pergunta brasil;
    private Pergunta franca;
    private Alternativa brasilia;
    private Alternativa paris;

    @BeforeEach
    void setUp() {
        atividade = new Atividade();
        atividade.setId(10L);
        atividade.setTipo(TipoAtividade.ASSOCIACAO);

        brasil = pergunta(1L, "Brasil");
        franca = pergunta(2L, "França");
        brasilia = par(11L, "Brasília", brasil);
        paris = par(21L, "Paris", franca);
    }

    @Test
    @DisplayName("criação: aceita pares válidos")
    void criacaoValida() {
        Atividade nova = new Atividade();
        assertDoesNotThrow(() -> template.validarQuestionario(nova, List.of(
                itemA("Brasil", "Brasília", true),
                itemA("França", "Paris", true)
        )));
    }

    @Test
    @DisplayName("criação: item com mais de um par é rejeitado")
    void criacaoComDoisPares() {
        PerguntaRequest item = new PerguntaRequest("Brasil", List.of(
                new AlternativaRequest("Brasília", true),
                new AlternativaRequest("Rio", true)
        ));
        assertErro("ASSOCIACAO_INVALIDA", () -> template.validarCriacao(item));
    }

    @Test
    @DisplayName("criação: par não marcado como correto é rejeitado")
    void criacaoParNaoCorreto() {
        assertErro("ASSOCIACAO_INVALIDA", () -> template.validarCriacao(itemA("Brasil", "Brasília", false)));
    }

    @Test
    @DisplayName("criação: precisa de pelo menos 2 pares")
    void criacaoComUmPar() {
        assertErro("ASSOCIACAO_INVALIDA", () -> template.validarQuestionario(new Atividade(), List.of(
                itemA("Brasil", "Brasília", true)
        )));
    }

    @Test
    @DisplayName("criação: texto repetido na coluna B é rejeitado (inclusive contra pares já salvos)")
    void criacaoComTextoRepetido() {
        assertErro("ASSOCIACAO_INVALIDA", () -> template.validarQuestionario(atividade, List.of(
                itemA("Capital federal", " brasília ", true)
        )));
    }

    @Test
    @DisplayName("visão do aluno: todas as perguntas recebem a coluna B completa e na mesma ordem")
    void perguntasParaAluno() {
        List<PerguntaResponse> perguntas = template.perguntasParaAluno(atividade);

        assertEquals(2, perguntas.size());
        Set<Long> ids = perguntas.get(0).alternativas().stream()
                .map(AlternativaResponse::id)
                .collect(Collectors.toSet());
        assertEquals(Set.of(11L, 21L), ids);
        assertEquals(perguntas.get(0).alternativas(), perguntas.get(1).alternativas());
        assertEquals(perguntas, template.perguntasParaAluno(atividade));
    }

    @Test
    @DisplayName("resposta: aceita alternativa de outra pergunta da mesma atividade")
    void respostaComParDeOutraPergunta() {
        assertDoesNotThrow(() -> template.validarResposta(brasil, new SalvarRespostaRequest(1L, 21L)));
    }

    @Test
    @DisplayName("resposta: rejeita alternativa que não é da atividade")
    void respostaForaDaAtividade() {
        assertErro("ALTERNATIVA_INVALIDA",
                () -> template.validarResposta(brasil, new SalvarRespostaRequest(1L, 999L)));
    }

    @Test
    @DisplayName("correção: certo quando liga ao próprio par, errado quando liga ao par de outro item")
    void correcao() {
        assertTrue(template.estaCorreta(brasil, resposta(brasilia)));
        assertFalse(template.estaCorreta(brasil, resposta(paris)));
        assertTrue(template.estaCorreta(franca, resposta(paris)));
    }

    private Pergunta pergunta(Long id, String enunciado) {
        Pergunta pergunta = new Pergunta();
        pergunta.setId(id);
        pergunta.setEnunciado(enunciado);
        pergunta.setAtividade(atividade);
        atividade.getPerguntas().add(pergunta);
        return pergunta;
    }

    private static Alternativa par(Long id, String texto, Pergunta pergunta) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setTexto(texto);
        alternativa.setCorreta(true);
        alternativa.setPergunta(pergunta);
        pergunta.getAlternativas().add(alternativa);
        return alternativa;
    }

    private static PerguntaRequest itemA(String enunciado, String par, boolean correta) {
        return new PerguntaRequest(enunciado, List.of(new AlternativaRequest(par, correta)));
    }

    private static RespostaAtividade resposta(Alternativa alternativa) {
        RespostaAtividade resposta = new RespostaAtividade();
        resposta.setAlternativa(alternativa);
        return resposta;
    }

    private static void assertErro(String codigo, Executable acao) {
        ApiException erro = assertThrows(ApiException.class, acao);
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        assertEquals(codigo, erro.getCodigo());
    }
}

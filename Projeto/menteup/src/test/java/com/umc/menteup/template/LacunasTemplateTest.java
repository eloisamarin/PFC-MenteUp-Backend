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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LacunasTemplateTest {

    private static final String TEXTO = "A capital do Brasil é {{1}} e a da França é {{2}}.";

    private final LacunasTemplate template = new LacunasTemplate();

    private Atividade atividade;
    private Pergunta pergunta;

    @BeforeEach
    void setUp() {
        atividade = new Atividade();
        atividade.setId(10L);

        pergunta = new Pergunta();
        pergunta.setId(1L);
        pergunta.setEnunciado(TEXTO);
        pergunta.setAtividade(atividade);
        atividade.getPerguntas().add(pergunta);

        palavra(11L, "Brasília", true, 1);
        palavra(12L, "Paris", true, 2);
        palavra(13L, "Roma", false, null);
    }

    @Test
    @DisplayName("criação: aceita texto com lacunas, palavras corretas e distratores")
    void criacaoValida() {
        assertDoesNotThrow(() -> template.validarCriacao(texto(TEXTO,
                new AlternativaRequest("Brasília", true, 1),
                new AlternativaRequest("Paris", true, 2),
                new AlternativaRequest("Roma", false)
        )));
    }

    @Test
    @DisplayName("criação: rejeita texto sem lacuna, numeração errada, palavra faltando e distrator com ordem")
    void criacaoInvalida() {
        assertErro("LACUNAS_INVALIDA", () -> template.validarCriacao(texto("Sem lacunas.",
                new AlternativaRequest("x", true, 1))));
        assertErro("LACUNAS_INVALIDA", () -> template.validarCriacao(texto("{{1}} e {{3}}",
                new AlternativaRequest("a", true, 1), new AlternativaRequest("b", true, 3))));
        assertErro("LACUNAS_INVALIDA", () -> template.validarCriacao(texto(TEXTO,
                new AlternativaRequest("Brasília", true, 1))));
        assertErro("LACUNAS_INVALIDA", () -> template.validarCriacao(texto(TEXTO,
                new AlternativaRequest("Brasília", true, 1),
                new AlternativaRequest("Paris", true, 2),
                new AlternativaRequest("Roma", false, 3))));
    }

    @Test
    @DisplayName("visão do aluno: texto com os marcadores e banco de palavras completo embaralhado")
    void perguntasParaAluno() {
        PerguntaResponse vista = template.perguntasParaAluno(atividade).get(0);
        List<Long> ids = vista.alternativas().stream().map(AlternativaResponse::id).toList();

        assertEquals(TEXTO, vista.texto());
        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of(11L, 12L, 13L)));
        assertNotEquals(List.of(11L, 12L, 13L), ids);
    }

    @Test
    @DisplayName("resposta: uma palavra por lacuna, sem repetir")
    void respostaInvalida() {
        assertErro("SEQUENCIA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L)));
        assertErro("SEQUENCIA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L, 11L)));
        assertErro("ALTERNATIVA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L, 99L)));
        assertDoesNotThrow(() -> template.validarResposta(pergunta, sequencia(13L, 12L)));
    }

    @Test
    @DisplayName("correção: acerta só com a palavra certa em cada lacuna")
    void correcao() {
        assertTrue(template.estaCorreta(pergunta, resposta(11L, 12L)));
        assertFalse(template.estaCorreta(pergunta, resposta(12L, 11L)));
        assertFalse(template.estaCorreta(pergunta, resposta(13L, 12L)));
    }

    @Test
    @DisplayName("correção: palavras iguais cadastradas duas vezes valem em qualquer lacuna correspondente")
    void correcaoComPalavrasRepetidas() {
        Pergunta repetida = new Pergunta();
        repetida.setId(2L);
        repetida.setEnunciado("{{1}} mais {{2}} é quatro.");
        Alternativa dois1 = palavraEm(repetida, 21L, "dois", 1);
        Alternativa dois2 = palavraEm(repetida, 22L, "dois", 2);

        assertTrue(template.estaCorreta(repetida, resposta(dois2.getId(), dois1.getId())));
    }

    private void palavra(Long id, String texto, boolean correta, Integer ordem) {
        Alternativa alternativa = palavraEm(pergunta, id, texto, ordem);
        alternativa.setCorreta(correta);
    }

    private static Alternativa palavraEm(Pergunta pergunta, Long id, String texto, Integer ordem) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setTexto(texto);
        alternativa.setCorreta(true);
        alternativa.setOrdem(ordem);
        alternativa.setPergunta(pergunta);
        pergunta.getAlternativas().add(alternativa);
        return alternativa;
    }

    private static PerguntaRequest texto(String enunciado, AlternativaRequest... palavras) {
        return new PerguntaRequest(enunciado, List.of(palavras));
    }

    private static SalvarRespostaRequest sequencia(Long... ids) {
        return new SalvarRespostaRequest(1L, null, List.of(ids));
    }

    private static RespostaAtividade resposta(Long... ids) {
        RespostaAtividade resposta = new RespostaAtividade();
        resposta.getAlternativasIds().addAll(List.of(ids));
        return resposta;
    }

    private static void assertErro(String codigo, Executable acao) {
        ApiException erro = assertThrows(ApiException.class, acao);
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        assertEquals(codigo, erro.getCodigo());
    }
}

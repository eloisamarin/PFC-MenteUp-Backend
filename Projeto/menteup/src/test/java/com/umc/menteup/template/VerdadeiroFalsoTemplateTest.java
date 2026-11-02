package com.umc.menteup.template;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.RespostaAtividade;
import com.umc.menteup.model.TipoAtividade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VerdadeiroFalsoTemplateTest {

    private final VerdadeiroFalsoTemplate template = new VerdadeiroFalsoTemplate();

    @Test
    @DisplayName("é registrado como VERDADEIRO_FALSO no resolver, separado do QUIZ")
    void registradoNoResolver() {
        TemplateResolver resolver = new TemplateResolver(List.of(new QuizTemplate(), template));

        assertSame(template, resolver.resolver(TipoAtividade.VERDADEIRO_FALSO));
        assertInstanceOf(QuizTemplate.class, resolver.resolver(TipoAtividade.QUIZ));
        assertNotSame(template, resolver.resolver(TipoAtividade.QUIZ));
    }

    @Test
    @DisplayName("criação: aceita afirmação com Verdadeiro e Falso e uma correta")
    void criacaoValida() {
        assertDoesNotThrow(() -> template.validarCriacao(afirmacao(
                new AlternativaRequest("Verdadeiro", true),
                new AlternativaRequest("Falso", false)
        )));
    }

    @Test
    @DisplayName("criação: rejeita quantidade diferente de 2 alternativas")
    void criacaoComTresAlternativas() {
        assertErro(() -> template.validarCriacao(afirmacao(
                new AlternativaRequest("Verdadeiro", true),
                new AlternativaRequest("Falso", false),
                new AlternativaRequest("Talvez", false)
        )));
    }

    @Test
    @DisplayName("criação: rejeita as duas alternativas corretas")
    void criacaoComDuasCorretas() {
        assertErro(() -> template.validarCriacao(afirmacao(
                new AlternativaRequest("Verdadeiro", true),
                new AlternativaRequest("Falso", true)
        )));
    }

    @Test
    @DisplayName("criação: rejeita nenhuma alternativa correta")
    void criacaoSemCorreta() {
        assertErro(() -> template.validarCriacao(afirmacao(
                new AlternativaRequest("Verdadeiro", false),
                new AlternativaRequest("Falso", false)
        )));
    }

    @Test
    @DisplayName("correção: reaproveita a regra do quiz")
    void correcao() {
        Pergunta pergunta = new Pergunta();
        pergunta.setId(1L);
        Alternativa verdadeiro = alternativa(11L, "Verdadeiro", true, pergunta);
        Alternativa falso = alternativa(12L, "Falso", false, pergunta);

        assertTrue(template.estaCorreta(pergunta, resposta(verdadeiro)));
        assertFalse(template.estaCorreta(pergunta, resposta(falso)));
    }

    private static PerguntaRequest afirmacao(AlternativaRequest... alternativas) {
        return new PerguntaRequest("O Sol é uma estrela.", List.of(alternativas));
    }

    private static Alternativa alternativa(Long id, String texto, boolean correta, Pergunta pergunta) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setTexto(texto);
        alternativa.setCorreta(correta);
        alternativa.setPergunta(pergunta);
        pergunta.getAlternativas().add(alternativa);
        return alternativa;
    }

    private static RespostaAtividade resposta(Alternativa alternativa) {
        RespostaAtividade resposta = new RespostaAtividade();
        resposta.setAlternativa(alternativa);
        return resposta;
    }

    private static void assertErro(org.junit.jupiter.api.function.Executable acao) {
        ApiException erro = assertThrows(ApiException.class, acao);
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatus());
        assertEquals("VERDADEIRO_FALSO_INVALIDO", erro.getCodigo());
    }
}

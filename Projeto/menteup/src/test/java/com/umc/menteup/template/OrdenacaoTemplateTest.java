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

class OrdenacaoTemplateTest {

    private final OrdenacaoTemplate template = new OrdenacaoTemplate();

    private Atividade atividade;
    private Pergunta pergunta;

    @BeforeEach
    void setUp() {
        atividade = new Atividade();
        atividade.setId(10L);

        pergunta = new Pergunta();
        pergunta.setId(1L);
        pergunta.setEnunciado("Ordene do menor para o maior");
        pergunta.setAtividade(atividade);
        atividade.getPerguntas().add(pergunta);

        item(11L, "1", 1);
        item(12L, "5", 2);
        item(13L, "10", 3);
    }

    @Test
    @DisplayName("criação: aceita itens com ordens de 1 a N")
    void criacaoValida() {
        assertDoesNotThrow(() -> template.validarCriacao(instrucao(
                new AlternativaRequest("1", true, 1),
                new AlternativaRequest("5", true, 2)
        )));
    }

    @Test
    @DisplayName("criação: rejeita menos de 2 itens, ordem ausente, repetida ou fora da faixa")
    void criacaoInvalida() {
        assertErro("ORDENACAO_INVALIDA", () -> template.validarCriacao(instrucao(
                new AlternativaRequest("1", true, 1))));
        assertErro("ORDENACAO_INVALIDA", () -> template.validarCriacao(instrucao(
                new AlternativaRequest("1", true, 1), new AlternativaRequest("5", true))));
        assertErro("ORDENACAO_INVALIDA", () -> template.validarCriacao(instrucao(
                new AlternativaRequest("1", true, 1), new AlternativaRequest("5", true, 1))));
        assertErro("ORDENACAO_INVALIDA", () -> template.validarCriacao(instrucao(
                new AlternativaRequest("1", true, 1), new AlternativaRequest("5", true, 3))));
    }

    @Test
    @DisplayName("visão do aluno: mesmos itens, fora da ordem cadastrada e estável entre chamadas")
    void perguntasParaAluno() {
        List<PerguntaResponse> perguntas = template.perguntasParaAluno(atividade);
        List<Long> ids = perguntas.get(0).alternativas().stream().map(AlternativaResponse::id).toList();

        assertEquals(3, ids.size());
        assertTrue(ids.containsAll(List.of(11L, 12L, 13L)));
        assertNotEquals(List.of(11L, 12L, 13L), ids);
        assertEquals(perguntas, template.perguntasParaAluno(atividade));
    }

    @Test
    @DisplayName("resposta: exige todos os itens, sem repetir e da própria pergunta")
    void respostaInvalida() {
        assertErro("SEQUENCIA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L, 12L)));
        assertErro("SEQUENCIA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L, 11L, 12L)));
        assertErro("SEQUENCIA_INVALIDA", () -> template.validarResposta(pergunta, new SalvarRespostaRequest(1L, 11L)));
        assertErro("ALTERNATIVA_INVALIDA", () -> template.validarResposta(pergunta, sequencia(11L, 12L, 99L)));
    }

    @Test
    @DisplayName("resposta: guarda a sequência e limpa a alternativa única")
    void preencherResposta() {
        RespostaAtividade resposta = new RespostaAtividade();
        template.preencherResposta(resposta, pergunta, sequencia(13L, 11L, 12L));

        assertNull(resposta.getAlternativa());
        assertEquals(List.of(13L, 11L, 12L), resposta.getAlternativasIds());
    }

    @Test
    @DisplayName("correção: só acerta com a sequência inteira correta")
    void correcao() {
        assertTrue(template.estaCorreta(pergunta, resposta(11L, 12L, 13L)));
        assertFalse(template.estaCorreta(pergunta, resposta(12L, 11L, 13L)));
    }

    private void item(Long id, String texto, int ordem) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setTexto(texto);
        alternativa.setCorreta(true);
        alternativa.setOrdem(ordem);
        alternativa.setPergunta(pergunta);
        pergunta.getAlternativas().add(alternativa);
    }

    private static PerguntaRequest instrucao(AlternativaRequest... itens) {
        return new PerguntaRequest("Ordene", List.of(itens));
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

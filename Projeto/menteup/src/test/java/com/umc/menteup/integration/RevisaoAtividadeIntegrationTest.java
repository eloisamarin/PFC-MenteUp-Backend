package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/*
 * Revisão das perguntas depois de enviar a atividade (GET /atividades/{id}/revisao):
 * - logo depois de enviar: a resposta do aluno e se acertou, sem o gabarito;
 * - depois que o professor encerra a atividade: também a resposta correta;
 * - antes de enviar, ou para quem não é aluno, a revisão não abre.
 */
class RevisaoAtividadeIntegrationTest extends IntegracaoBase {

    private record Cenario(Sessao professor, Sessao aluno, Long turmaId, Long atividadeId,
                           List<PerguntaCriada> perguntas) {
    }

    private Cenario cenario() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, 2);
        return new Cenario(professor, aluno, turmaId, atividadeId, perguntas);
    }

    @Test
    @SuppressWarnings("unchecked")
    void revisar_logoDepoisDeEnviar_deveMostrarARespostaEOAcertoSemOGabarito() {
        // Arrange: o aluno erra a primeira pergunta e acerta a segunda
        Cenario c = cenario();
        responder(c.aluno(), c.atividadeId(), c.perguntas().get(0).id(), c.perguntas().get(0).alternativas().get(1));
        responder(c.aluno(), c.atividadeId(), c.perguntas().get(1).id(), c.perguntas().get(1).alternativas().get(0));
        enviar(c.aluno(), c.atividadeId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = revisar(c.aluno(), c.atividadeId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("gabaritoLiberado", false);
        List<Map<String, Object>> questoes = (List<Map<String, Object>>) resposta.getBody().get("questoes");
        assertThat(questoes).hasSize(2);
        assertThat(questoes.get(0))
                .containsEntry("enunciado", "Pergunta 1")
                .containsEntry("suaResposta", List.of("Errada 1"))
                .containsEntry("acertou", false)
                .containsEntry("respostaCorreta", null);
        assertThat(questoes.get(1))
                .containsEntry("suaResposta", List.of("Certa 2"))
                .containsEntry("acertou", true)
                .containsEntry("respostaCorreta", null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void revisar_depoisQueOProfessorEncerra_deveMostrarTambemARespostaCorreta() {
        // Arrange
        Cenario c = cenario();
        concluirAtividade(c.aluno(), c.atividadeId(), c.perguntas(), false);
        encerrar(c.professor(), c.atividadeId(), c.turmaId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = revisar(c.aluno(), c.atividadeId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("gabaritoLiberado", true);
        List<Map<String, Object>> questoes = (List<Map<String, Object>>) resposta.getBody().get("questoes");
        assertThat(questoes.get(0))
                .containsEntry("suaResposta", List.of("Errada 1"))
                .containsEntry("acertou", false)
                .containsEntry("respostaCorreta", List.of("Certa 1"));
    }

    @Test
    void revisar_antesDeEnviar_deveRetornar409ComCodigo() {
        Cenario c = cenario();
        responder(c.aluno(), c.atividadeId(), c.perguntas().get(0).id(), c.perguntas().get(0).alternativas().get(0));

        ResponseEntity<Map<String, Object>> resposta = revisar(c.aluno(), c.atividadeId());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENVIADA");
    }

    @Test
    void revisar_comoProfessor_deveRetornar403ComCodigo() {
        Cenario c = cenario();

        ResponseEntity<Map<String, Object>> resposta = revisar(c.professor(), c.atividadeId());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_ALUNO");
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> revisar(Sessao sessao, Long atividadeId) {
        return client.get().uri("/atividades/{id}/revisao", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private void enviar(Sessao aluno, Long atividadeId) {
        ResponseEntity<Map<String, Object>> envio = client.post().uri("/atividades/{id}/respostas", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(JSON);
        assertThat(envio.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    /// O professor encerra a atividade (status CONCLUIDO), o que libera o gabarito na revisão
    private void encerrar(Sessao professor, Long atividadeId, Long turmaId) {
        Map<String, Object> corpo = new HashMap<>(buildAtividade(nomeUnico("Atividade"), turmaId, "CONCLUIDO"));
        corpo.put("id", atividadeId);
        ResponseEntity<Void> edicao = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();
        assertThat(edicao.getStatusCode().is2xxSuccessful()).isTrue();
    }
}

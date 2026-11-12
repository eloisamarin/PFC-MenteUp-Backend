package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/*
 * Testes de integração dos pedidos de entrada e saída de turma:
 * o aluno solicita, o professor da turma (ou um administrador) aprova ou recusa.
 */
class SolicitacaoTurmaIntegrationTest extends IntegracaoBase {

    // ------------------------------------------------------------- Pedido de entrada

    @Test
    void solicitarEntrada_deveFicarPendenteSemMatricular() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = solicitar(aluno, turmaId, "ENTRADA");

        // Assert: o pedido existe, mas o aluno ainda não está na turma
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("status", "PENDENTE").containsEntry("tipo", "ENTRADA");
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();

        assertThat(turmaDoAluno(aluno, turmaId))
                .containsEntry("matriculado", false)
                .containsEntry("solicitacaoPendenteTipo", "ENTRADA");
    }

    @Test
    void aprovarEntrada_peloProfessorDaTurma_deveMatricularOAluno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        // Act
        ResponseEntity<Void> resposta = responder(professor, turmaId, solicitacaoId, "aprovar");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(estaNaTurma(aluno, turmaId)).isTrue();
        assertThat(minhaSolicitacao(aluno, solicitacaoId)).containsEntry("status", "APROVADA");
        assertThat(pendentes(professor, turmaId).getBody()).isEmpty();
    }

    @Test
    void recusarEntrada_naoDeveMatricular() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        // Act
        ResponseEntity<Void> resposta = responder(professor, turmaId, solicitacaoId, "recusar");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
        assertThat(minhaSolicitacao(aluno, solicitacaoId)).containsEntry("status", "RECUSADA");
    }

    @Test
    void administrador_deveAprovarPedidoDeQualquerTurma() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        // Act + Assert
        assertThat(pendentes(administrador, turmaId).getBody()).hasSize(1);
        assertThat(responder(administrador, turmaId, solicitacaoId, "aprovar").getStatusCode())
                .isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(estaNaTurma(aluno, turmaId)).isTrue();
    }

    // ------------------------------------------------------------- Pedido de saída

    @Test
    void aprovarSaida_deveDesvincularOAluno() {
        // Arrange: o professor vincula e depois o aluno pede para sair
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long solicitacaoId = id(solicitar(aluno, turmaId, "SAIDA").getBody());

        // Ainda está na turma enquanto o pedido não é aprovado
        assertThat(estaNaTurma(aluno, turmaId)).isTrue();

        // Act
        responder(professor, turmaId, solicitacaoId, "aprovar");

        // Assert
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    // ------------------------------------------------------------- Regras

    @Test
    void solicitar_comPedidoJaAberto_deveRetornar409() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        solicitar(aluno, turmaId, "ENTRADA");

        ResponseEntity<Map<String, Object>> resposta = solicitar(aluno, turmaId, "ENTRADA");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOLICITACAO_PENDENTE");
    }

    @Test
    void solicitar_entradaJaMatriculadoOuSaidaSemEstar_deveRetornar409() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaComAluno = criarTurma(professor);
        Long turmaSemAluno = criarTurma(professor);
        vincularAluno(professor, turmaComAluno, aluno.id());

        assertThat(solicitar(aluno, turmaComAluno, "ENTRADA").getBody()).containsEntry("codigo", "JA_MATRICULADO");
        assertThat(solicitar(aluno, turmaSemAluno, "SAIDA").getBody()).containsEntry("codigo", "NAO_MATRICULADO");
    }

    @Test
    void solicitar_semTipo_deveRetornar400() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/alunos/me/solicitacoes")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("turmaId", turmaId))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    @Test
    void cancelar_peloAluno_deveImpedirAAprovacao() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        // Act
        ResponseEntity<Void> cancelamento = client.delete().uri("/alunos/me/solicitacoes/{id}", solicitacaoId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(cancelamento.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(minhaSolicitacao(aluno, solicitacaoId)).containsEntry("status", "CANCELADA");
        assertThat(responder(professor, turmaId, solicitacaoId, "aprovar").getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    @Test
    void outroProfessor_naoDeveVerNemResponderPedidos() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        ResponseEntity<Void> lista = client.get().uri("/turmas/{turmaId}/solicitacoes", turmaId)
                .header(HttpHeaders.AUTHORIZATION, outroProfessor.token())
                .retrieve()
                .toBodilessEntity();

        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(responder(outroProfessor, turmaId, solicitacaoId, "aprovar").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    @Test
    void aluno_naoDeveAprovarOProprioPedido() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long solicitacaoId = id(solicitar(aluno, turmaId, "ENTRADA").getBody());

        assertThat(responder(aluno, turmaId, solicitacaoId, "aprovar").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    @Test
    void aluno_naoDeveMaisEntrarNaTurmaSemPedido() {
        // A rota antiga de entrar direto (POST /alunos/me/turmas/{id}) não existe mais
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        ResponseEntity<Void> resposta = client.post().uri("/alunos/me/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode().is4xxClientError()).isTrue();
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    @Test
    void professor_naoDeveVincularAlunoEmTurmaDeOutroProfessor() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        ResponseEntity<Map<String, Object>> resposta = client.post()
                .uri("/turmas/{turmaId}/alunos/{alunoId}", turmaId, aluno.id())
                .header(HttpHeaders.AUTHORIZATION, outroProfessor.token())
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_ACESSO_A_TURMA");
        assertThat(estaNaTurma(aluno, turmaId)).isFalse();
    }

    @Test
    void excluirTurma_comPedidoPendente_deveFuncionar() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        solicitar(aluno, turmaId, "ENTRADA");

        ResponseEntity<Void> resposta = client.delete().uri("/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(minhasSolicitacoes(aluno)).isEmpty();
    }

    @Test
    void turmasDisponiveis_naoDeveExporOsAlunosDaTurma() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Sessao colega = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, colega.id());

        String resposta = client.get().uri("/alunos/me/turmas/disponiveis")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(String.class)
                .getBody();

        assertThat(resposta).doesNotContain(colega.email()).doesNotContain("\"alunos\"");
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> solicitar(Sessao aluno, Long turmaId, String tipo) {
        return client.post().uri("/alunos/me/solicitacoes")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("turmaId", turmaId, "tipo", tipo))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Void> responder(Sessao sessao, Long turmaId, Long solicitacaoId, String acao) {
        return client.put().uri("/turmas/{turmaId}/solicitacoes/{id}/{acao}", turmaId, solicitacaoId, acao)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();
    }

    private ResponseEntity<List<Map<String, Object>>> pendentes(Sessao sessao, Long turmaId) {
        return client.get().uri("/turmas/{turmaId}/solicitacoes", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    private List<Map<String, Object>> minhasSolicitacoes(Sessao aluno) {
        return client.get().uri("/alunos/me/solicitacoes")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();
    }

    private Map<String, Object> minhaSolicitacao(Sessao aluno, Long solicitacaoId) {
        return minhasSolicitacoes(aluno).stream()
                .filter(s -> solicitacaoId.equals(id(s)))
                .findFirst()
                .orElseThrow();
    }

    private Map<String, Object> turmaDoAluno(Sessao aluno, Long turmaId) {
        return client.get().uri("/alunos/me/turmas/disponiveis")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody()
                .stream()
                .filter(turma -> turmaId.equals(id(turma)))
                .findFirst()
                .orElseThrow();
    }

    private boolean estaNaTurma(Sessao aluno, Long turmaId) {
        return client.get().uri("/alunos/me/turmas")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody()
                .stream()
                .anyMatch(turma -> turmaId.equals(id(turma)));
    }
}

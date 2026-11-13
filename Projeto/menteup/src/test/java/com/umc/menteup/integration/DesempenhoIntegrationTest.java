package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/*
 * Testes de integração do acompanhamento de desempenho (RF30).
 * Cada teste cria os próprios usuários, turmas e atividades pelos endpoints reais,
 * então nenhum depende de outro.
 */
class DesempenhoIntegrationTest extends IntegracaoBase {

    // ------------------------------------------------- GET /turmas/{id}/desempenho

    @Test
    @SuppressWarnings("unchecked")
    void buscarDesempenho_comoProfessorDaTurma_deveRetornar200ComOsNumerosDoAluno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, 2);
        concluirAtividade(aluno, atividadeId, perguntas, true);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(professor, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("totalAlunos", 1)
                .containsEntry("totalAtividades", 1)
                .containsEntry("mediaPontuacao", 10.0)
                .containsEntry("taxaParticipacao", 100);

        List<Map<String, Object>> alunos = (List<Map<String, Object>>) resposta.getBody().get("alunos");
        assertThat(alunos).hasSize(1);
        assertThat(alunos.get(0))
                .containsEntry("pontuacao", 10)
                .containsEntry("acertos", 2)
                .containsEntry("atividadesConcluidas", 1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarDesempenho_comAlunoSemRespostas_deveMostrarOAlunoComZero() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao alunoQueFez = novaSessao("ALUNO");
        Sessao alunoParado = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, alunoQueFez.id());
        vincularAluno(professor, turmaId, alunoParado.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        concluirAtividade(alunoQueFez, atividadeId, cadastrarQuestionario(professor, atividadeId, 2), true);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(professor, turmaId);

        // Assert
        assertThat(resposta.getBody()).containsEntry("taxaParticipacao", 50);

        List<Map<String, Object>> alunos = (List<Map<String, Object>>) resposta.getBody().get("alunos");
        assertThat(alunos).hasSize(2);

        // A lista vem ordenada pela pontuação: quem não fez nada fica por último
        Map<String, Object> ultimo = alunos.get(1);
        assertThat(((Number) ultimo.get("alunoId")).longValue()).isEqualTo(alunoParado.id());
        assertThat(ultimo)
                .containsEntry("pontuacao", 0)
                .containsEntry("acertos", 0)
                .containsEntry("atividadesConcluidas", 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarDesempenho_comTurmaVazia_deveRetornar200ComZeros() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(professor, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("totalAlunos", 0)
                .containsEntry("totalAtividades", 0)
                .containsEntry("mediaPontuacao", 0.0)
                .containsEntry("taxaParticipacao", 0);
        assertThat((List<Object>) resposta.getBody().get("alunos")).isEmpty();
    }

    @Test
    void buscarDesempenho_comoOutroProfessor_deveRetornar403ComCodigo() {
        // Arrange
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(dono);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(outroProfessor, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_ACESSO_A_TURMA");
    }

    @Test
    void buscarDesempenho_comoAluno_deveRetornar403() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(aluno, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_ACESSO_A_TURMA");
    }

    @Test
    void buscarDesempenho_comoAdministrador_deveRetornar200EmQualquerTurma() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(administrador, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) resposta.getBody().get("turmaId")).longValue()).isEqualTo(turmaId);
    }

    @Test
    void buscarDesempenho_comTurmaInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarDesempenho(professor, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void buscarDesempenho_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri("/turmas/{id}/desempenho", 1L)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------- GET /turmas/minhas

    @Test
    void minhasTurmas_comoProfessor_deveTrazerSoAsTurmasDele() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Long minhaTurma = criarTurma(professor);
        Long turmaDeOutro = criarTurma(outroProfessor);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = minhasTurmas(professor);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(IntegracaoBase::id)
                .containsExactly(minhaTurma)
                .doesNotContain(turmaDeOutro);
    }

    @Test
    void minhasTurmas_comoAluno_deveRetornar403ComCodigo() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act: em caso de erro, a resposta é um objeto, e não uma lista
        ResponseEntity<Map<String, Object>> resposta = client.get().uri("/turmas/minhas")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
    }

    // ------------------------------------------------------------------ Helpers

    /** Responde todas as perguntas e conclui a atividade, gerando o resultado do aluno. */
    private ResponseEntity<Map<String, Object>> buscarDesempenho(Sessao sessao, Long turmaId) {
        return client.get().uri("/turmas/{id}/desempenho", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<List<Map<String, Object>>> minhasTurmas(Sessao sessao) {
        return client.get().uri("/turmas/minhas")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }
}
package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;

// Ranking por turma: cada um só vê o ranking das turmas a que tem acesso
class RankingIntegrationTest extends IntegracaoBase {

    // Professor com duas turmas: o aluno e o colega na turma A, o aluno de fora na turma B
    private record Cenario(Sessao professor, Sessao aluno, Sessao colega, Sessao deFora, Long turmaA, Long turmaB) {
    }

    private Cenario cenario() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Sessao colega = novaSessao("ALUNO");
        Sessao deFora = novaSessao("ALUNO");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);
        vincularAluno(professor, turmaA, aluno.id());
        vincularAluno(professor, turmaA, colega.id());
        vincularAluno(professor, turmaB, deFora.id());

        concluirUmaAtividade(professor, turmaA, aluno);
        concluirUmaAtividade(professor, turmaA, colega);
        concluirUmaAtividade(professor, turmaB, deFora);

        return new Cenario(professor, aluno, colega, deFora, turmaA, turmaB);
    }

    private void concluirUmaAtividade(Sessao professor, Long turmaId, Sessao aluno) {
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, 1);
        concluirAtividade(aluno, atividadeId, perguntas, true);
    }

    @Test
    void rankingDaTurma_comoAluno_deveTrazerSoOsColegasDaTurma() {
        Cenario c = cenario();

        ResponseEntity<List<Map<String, Object>>> resposta = ranking(c.aluno(), c.turmaA());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDe(resposta)).containsExactlyInAnyOrder(c.aluno().id(), c.colega().id());
    }

    @Test
    void rankingDeOutraTurma_comoAluno_deveRetornar403() {
        Cenario c = cenario();

        assertThat(statusDoRanking(c.aluno(), c.turmaB())).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rankingDaTurma_deOutroProfessor_deveRetornar403() {
        Cenario c = cenario();
        Sessao outroProfessor = novaSessao("PROFESSOR");

        assertThat(statusDoRanking(outroProfessor, c.turmaA())).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void rankingSemTurma_comoAluno_naoDeveTrazerAlunoDeOutraTurma() {
        Cenario c = cenario();

        ResponseEntity<List<Map<String, Object>>> resposta = ranking(c.aluno(), null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDe(resposta)).contains(c.aluno().id(), c.colega().id()).doesNotContain(c.deFora().id());
    }

    @Test
    void rankingSemTurma_comoProfessor_deveTrazerOsAlunosDasTurmasDele() {
        Cenario c = cenario();

        ResponseEntity<List<Map<String, Object>>> resposta = ranking(c.professor(), null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDe(resposta)).containsExactlyInAnyOrder(c.aluno().id(), c.colega().id(), c.deFora().id());
    }

    @Test
    void ranking_semToken_deveRetornar401() {
        ResponseEntity<Void> resposta = client.get().uri("/ranking")
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<List<Map<String, Object>>> ranking(Sessao sessao, Long turmaId) {
        return client.get().uri(uriDoRanking(turmaId))
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    // Nas recusas a resposta é um objeto de erro, e não uma lista: aqui só importa o status
    private HttpStatusCode statusDoRanking(Sessao sessao, Long turmaId) {
        return client.get().uri(uriDoRanking(turmaId))
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity()
                .getStatusCode();
    }

    private static String uriDoRanking(Long turmaId) {
        return turmaId == null ? "/ranking" : "/ranking?turmaId=" + turmaId;
    }

    private static List<Long> idsDe(ResponseEntity<List<Map<String, Object>>> resposta) {
        return resposta.getBody().stream()
                .map(r -> r.get("usuarioId") == null ? null : ((Number) r.get("usuarioId")).longValue())
                .toList();
    }
}

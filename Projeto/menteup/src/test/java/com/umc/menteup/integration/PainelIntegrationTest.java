package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/*
 * Painel do sistema (GET /dashboard/painel) com filtro de período, turma e aluno:
 * - aluno: só os próprios dados;
 * - professor: as turmas em que é o responsável e os alunos dessas turmas;
 * - administrador: tudo.
 *
 * Cenário: o professor tem a turma A (alunos 1 e 2) e outro professor tem a turma B (aluno 3).
 * Os alunos 1 e 3 concluem uma atividade cada; o aluno 2 ainda não fez nada.
 */
class PainelIntegrationTest extends IntegracaoBase {

    private Sessao professor;
    private Sessao outroProfessor;
    private Sessao aluno1;
    private Sessao aluno2;
    private Sessao aluno3;
    private Long turmaA;
    private Long turmaB;

    @BeforeEach
    void montarCenario() {
        professor = novaSessao("PROFESSOR");
        outroProfessor = novaSessao("PROFESSOR");
        aluno1 = novaSessao("ALUNO");
        aluno2 = novaSessao("ALUNO");
        aluno3 = novaSessao("ALUNO");

        turmaA = criarTurma(professor);
        vincularAluno(professor, turmaA, aluno1.id());
        vincularAluno(professor, turmaA, aluno2.id());
        turmaB = criarTurma(outroProfessor);
        vincularAluno(outroProfessor, turmaB, aluno3.id());

        Long atividadeA = criarAtividade(professor, turmaA, "EM_ANDAMENTO");
        concluirAtividade(aluno1, atividadeA, cadastrarQuestionario(professor, atividadeA, 2), true);
        Long atividadeB = criarAtividade(outroProfessor, turmaB, "EM_ANDAMENTO");
        concluirAtividade(aluno3, atividadeB, cadastrarQuestionario(outroProfessor, atividadeB, 2), false);
    }

    // ------------------------------------------------------------- Aluno

    @Test
    void aluno_deveVerSoOsProprioDados() {
        Map<String, Object> painel = painel(aluno1, "").getBody();

        assertThat(painel).containsEntry("perfil", "ALUNO");
        assertThat(resumo(painel)).containsEntry("atividadesConcluidas", 1).containsEntry("acertoAtividades", 100.0);
        assertThat(ids(painel, "turmas")).containsExactly(turmaA);
        assertThat(lista(painel, "alunos")).isEmpty();
        assertThat(lista(painel, "desempenhoAlunos")).isEmpty();
    }

    @Test
    void aluno_naoDeveVerOutroAlunoNemTurmaQueNaoEDele() {
        assertThat(painel(aluno1, "&alunoId=" + aluno3.id()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(painel(aluno1, "&turmaId=" + turmaB).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void aluno_queAindaNaoFezNada_deveVerZero() {
        Map<String, Object> painel = painel(aluno2, "").getBody();

        assertThat(resumo(painel)).containsEntry("atividadesConcluidas", 0);
        assertThat(resumo(painel).get("acertoAtividades")).isNull();
    }

    // ------------------------------------------------------------- Professor

    @Test
    void professor_deveVerSoAsTurmasDeleEOsAlunosDelas() {
        Map<String, Object> painel = painel(professor, "").getBody();

        assertThat(painel).containsEntry("perfil", "PROFESSOR");
        assertThat(ids(painel, "turmas")).containsExactly(turmaA);
        assertThat(ids(painel, "alunos")).containsExactlyInAnyOrder(aluno1.id(), aluno2.id());
        // Só o resultado do aluno 1 (o do aluno 3 é da turma de outro professor)
        assertThat(resumo(painel)).containsEntry("atividadesConcluidas", 1).containsEntry("alunos", 2);
        // O aluno 2 aparece na tabela mesmo sem ter feito nada
        assertThat(ids(painel, "desempenhoAlunos")).containsExactlyInAnyOrder(aluno1.id(), aluno2.id());
    }

    @Test
    void professor_naoDeveVerTurmaNemAlunoDeOutroProfessor() {
        ResponseEntity<Map<String, Object>> turma = painel(professor, "&turmaId=" + turmaB);
        ResponseEntity<Map<String, Object>> aluno = painel(professor, "&alunoId=" + aluno3.id());

        assertThat(turma.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(turma.getBody()).containsEntry("codigo", "SEM_ACESSO_A_TURMA");
        assertThat(aluno.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(aluno.getBody()).containsEntry("codigo", "SEM_ACESSO_AO_ALUNO");
    }

    @Test
    void professor_filtrandoUmAluno_deveVerSoEle() {
        Map<String, Object> painel = painel(professor, "&alunoId=" + aluno2.id()).getBody();

        assertThat(resumo(painel)).containsEntry("atividadesConcluidas", 0);
        assertThat(ids(painel, "desempenhoAlunos")).containsExactly(aluno2.id());
    }

    @Test
    void professorSemTurma_deveVerPainelVazio() {
        Map<String, Object> painel = painel(novaSessao("PROFESSOR"), "").getBody();

        assertThat(lista(painel, "turmas")).isEmpty();
        assertThat(resumo(painel)).containsEntry("atividadesConcluidas", 0);
    }

    // ------------------------------------------------------------- Administrador

    @Test
    void administrador_deveVerTudoEPoderFiltrarQualquerTurma() {
        Sessao administrador = novaSessao("ADMINISTRADOR");

        Map<String, Object> geral = painel(administrador, "").getBody();
        Map<String, Object> soTurmaB = painel(administrador, "&turmaId=" + turmaB).getBody();

        assertThat(geral).containsEntry("perfil", "ADMINISTRADOR");
        assertThat(ids(geral, "turmas")).contains(turmaA, turmaB);
        assertThat(((Number) resumo(geral).get("atividadesConcluidas")).longValue()).isGreaterThanOrEqualTo(2);
        assertThat(ids(soTurmaB, "alunos")).containsExactly(aluno3.id());
        assertThat(resumo(soTurmaB)).containsEntry("atividadesConcluidas", 1).containsEntry("acertoAtividades", 0.0);
    }

    // ------------------------------------------------------------- Período

    @Test
    void periodo_deveTerUmPontoPorDiaEAceitarSoOsValoresDoFiltro() {
        Map<String, Object> painel = painel(professor, "dias=7").getBody();

        assertThat(lista(painel, "conclusoesPorDia")).hasSize(7);
        assertThat(painel(professor, "dias=12").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void tokenDoGrafana_naoDeveAbrirOPainel() {
        ResponseEntity<Map<String, Object>> resposta = client.get().uri("/dashboard/painel")
                .header("X-Dashboard-Token", "qualquer-coisa")
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> painel(Sessao sessao, String filtros) {
        String periodo = filtros.contains("dias=") ? "" : "dias=30";
        return client.get().uri("/dashboard/painel?" + periodo + filtros)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> resumo(Map<String, Object> painel) {
        return (Map<String, Object>) painel.get("resumo");
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> lista(Map<String, Object> painel, String campo) {
        return (List<Map<String, Object>>) painel.get(campo);
    }

    private static List<Long> ids(Map<String, Object> painel, String campo) {
        return lista(painel, campo).stream().map(IntegracaoBase::id).toList();
    }
}

package com.umc.menteup.integration;


import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/*
 - Testes de integração da API da dashboard (/dashboard/**), lida pelo Grafana.
 - Acesso: administrador logado ou o token da dashboard (application-test.properties).
 - O banco é compartilhado entre as classes de teste, então os Big Numbers
  são conferidos pela DIFERENÇA antes/depois e não por valores fixos.
 */
class DashboardIntegrationTest extends IntegracaoBase {

    private static final String TOKEN = "token-da-dashboard-de-teste";
    private static final String CABECALHO = "X-Dashboard-Token";

    // ------------------------------------------------------------- Acesso

    @Test
    void resumo_semAutenticacao_deveRetornar401() {
        ResponseEntity<Void> resposta = client.get().uri("/dashboard/resumo")
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void resumo_comoAlunoOuProfessor_deveRetornar403() {
        Sessao aluno = novaSessao("ALUNO");
        Sessao professor = novaSessao("PROFESSOR");

        assertThat(comLogin(aluno, "/dashboard/resumo").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(comLogin(professor, "/dashboard/resumo").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void resumo_comoAdministrador_deveRetornar200() {
        Sessao administrador = novaSessao("ADMINISTRADOR");

        assertThat(comLogin(administrador, "/dashboard/resumo").getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void resumo_comTokenDaDashboard_deveRetornar200() {
        ResponseEntity<Map<String, Object>> resposta = comToken(TOKEN, "/dashboard/resumo");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsKeys(
                "alunos", "professores", "turmas", "atividadesCriadas", "atividadesConcluidas",
                "acertoAtividades", "desafiosRealizados", "acertoDesafios");
    }

    @Test
    void resumo_comTokenErrado_deveRetornar401() {
        assertThat(comToken("token-errado", "/dashboard/resumo").getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenDaDashboard_naoDeveAbrirOutrasRotas() {
        // O token só vale para /dashboard: não serve para listar usuários, por exemplo
        ResponseEntity<Void> resposta = client.get().uri("/usuarios/all")
                .header(CABECALHO, TOKEN)
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------- Indicadores

    @Test
    void resumo_deveContarOQueFoiCriadoEConcluido() {
        // Arrange
        Map<String, Object> antes = comToken(TOKEN, "/dashboard/resumo").getBody();
        Cenario cenario = alunoConcluiAtividadeComMetadeDosAcertos();

        // Act
        Map<String, Object> depois = comToken(TOKEN, "/dashboard/resumo").getBody();

        // Assert: +1 aluno, +1 professor, +1 turma, +1 atividade criada e +1 concluída
        assertThat(cenario).isNotNull();
        assertThat(diferenca(antes, depois, "alunos")).isEqualTo(1);
        assertThat(diferenca(antes, depois, "professores")).isEqualTo(1);
        assertThat(diferenca(antes, depois, "turmas")).isEqualTo(1);
        assertThat(diferenca(antes, depois, "atividadesCriadas")).isEqualTo(1);
        assertThat(diferenca(antes, depois, "atividadesConcluidas")).isEqualTo(1);
        assertThat(depois.get("acertoAtividades")).isNotNull();
    }

    @Test
    void acertoPorTurma_deveMostrarOPercentualDaTurma() {
        // Arrange: 1 acerto em 2 perguntas
        Cenario cenario = alunoConcluiAtividadeComMetadeDosAcertos();

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = listaComToken("/dashboard/acerto-por-turma");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .filteredOn(item -> cenario.turma().equals(item.get("turma")))
                .singleElement()
                .satisfies(item -> assertThat(((Number) item.get("acerto")).doubleValue()).isEqualTo(50.0));
    }

    @Test
    void alunosPorTurma_deveContarOsMatriculados() {
        // Arrange
        Cenario cenario = alunoConcluiAtividadeComMetadeDosAcertos();

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = listaComToken("/dashboard/alunos-por-turma");

        // Assert
        assertThat(resposta.getBody())
                .filteredOn(item -> cenario.turma().equals(item.get("nome")))
                .singleElement()
                .satisfies(item -> assertThat(((Number) item.get("quantidade")).longValue()).isEqualTo(1));
    }

    @Test
    void atividadesConcluidasPorDia_semPeriodo_deveTrazer30DiasComHojeNoFim() {
        // Arrange
        alunoConcluiAtividadeComMetadeDosAcertos();
        String hoje = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = listaComToken("/dashboard/atividades-concluidas-por-dia");

        // Assert: um ponto por dia (inclusive os dias sem nada), o último é hoje
        assertThat(resposta.getBody()).hasSize(30);
        Map<String, Object> ultimo = resposta.getBody().get(29);
        assertThat((String) ultimo.get("dia")).startsWith(hoje).endsWith("-03:00");
        assertThat(((Number) ultimo.get("concluidas")).longValue()).isPositive();
    }

    @Test
    void seriesPorDia_comPeriodoDoGrafana_deveRespeitarODeEOAte() {
        // Arrange: 7 dias, em milissegundos (como o Grafana manda)
        long ate = System.currentTimeMillis();
        long de = ate - 6L * 24 * 60 * 60 * 1000;
        String periodo = "?de=" + de + "&ate=" + ate;

        // Act + Assert
        assertThat(listaComToken("/dashboard/desafios-por-dia" + periodo).getBody()).hasSize(7);
        assertThat(listaComToken("/dashboard/acoes-por-dia" + periodo).getBody()).hasSize(7);
    }

    @Test
    void seriesPorDia_comInicioDepoisDoFim_deveRetornar400() {
        long agora = System.currentTimeMillis();

        ResponseEntity<Map<String, Object>> resposta =
                comToken(TOKEN, "/dashboard/acoes-por-dia?de=" + agora + "&ate=" + (agora - 86_400_000L * 3));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "PERIODO_INVALIDO");
    }

    @Test
    void atividadesPorTemplateEStatus_deveTrazerOsNomesTraduzidos() {
        alunoConcluiAtividadeComMetadeDosAcertos();

        assertThat(listaComToken("/dashboard/atividades-por-template").getBody())
                .extracting(item -> item.get("nome"))
                .contains("Quiz");
        assertThat(listaComToken("/dashboard/atividades-por-status").getBody())
                .extracting(item -> item.get("nome"))
                .contains("Em andamento");
    }

    @Test
    void api_naoDeveExporDadosPessoais() {
        // Arrange
        Cenario cenario = alunoConcluiAtividadeComMetadeDosAcertos();

        // Act
        String tudo = String.join("\n",
                texto("/dashboard/resumo"), texto("/dashboard/acerto-por-turma"),
                texto("/dashboard/alunos-por-turma"), texto("/dashboard/avatares"));

        // Assert
        assertThat(tudo).doesNotContain(cenario.emailAluno()).doesNotContain("Usuário Teste");
    }

    // ------------------------------------------------------------- Helpers

    private record Cenario(String turma, String emailAluno) {
    }

    // Professor cria turma e atividade de 2 perguntas; o aluno acerta 1 e conclui
    private Cenario alunoConcluiAtividadeComMetadeDosAcertos() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, 2);

        // A primeira alternativa é a certa (regra do cadastrarQuestionario)
        responder(aluno, atividadeId, perguntas.get(0).id(), perguntas.get(0).alternativas().get(0));
        responder(aluno, atividadeId, perguntas.get(1).id(), perguntas.get(1).alternativas().get(1));

        ResponseEntity<Map<String, Object>> conclusao = client.post()
                .uri("/atividades/{id}/respostas", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(JSON);
        assertThat(conclusao.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String nomeTurma = (String) buscarTurma(professor, turmaId).get("nome");
        return new Cenario(nomeTurma, aluno.email());
    }

    private Map<String, Object> buscarTurma(Sessao sessao, Long turmaId) {
        return client.get().uri("/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON)
                .getBody();
    }

    private static long diferenca(Map<String, Object> antes, Map<String, Object> depois, String campo) {
        return ((Number) depois.get(campo)).longValue() - ((Number) antes.get(campo)).longValue();
    }

    private ResponseEntity<Map<String, Object>> comToken(String token, String caminho) {
        return client.get().uri(caminho)
                .header(CABECALHO, token)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<List<Map<String, Object>>> listaComToken(String caminho) {
        return client.get().uri(caminho)
                .header(CABECALHO, TOKEN)
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    private ResponseEntity<Void> comLogin(Sessao sessao, String caminho) {
        return client.get().uri(caminho)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();
    }

    private String texto(String caminho) {
        return client.get().uri(caminho)
                .header(CABECALHO, TOKEN)
                .retrieve()
                .toEntity(String.class)
                .getBody();
    }
}


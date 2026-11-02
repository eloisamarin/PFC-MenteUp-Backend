package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class LogSistemaControllerIntegrationTest extends IntegracaoBase {

    private static final String BASE_PATH = "/logs";

    private ResponseEntity<Map<String, Object>> listar(Sessao sessao) {
        return client.get().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    @Test
    void listar_comoAluno_deveRetornar403() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = listar(novaSessao("ALUNO"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_comoProfessor_deveRetornar403() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = listar(novaSessao("PROFESSOR"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void listar_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri(BASE_PATH).retrieve().toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listar_comoAdministrador_deveRetornar200ComPagina() {
        // Arrange: a criação da turma grava uma entrada de log com o autor
        Sessao administrador = novaSessao("ADMINISTRADOR");
        criarTurma(administrador);

        // Act
        ResponseEntity<Map<String, Object>> resposta = listar(administrador);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsKey("content");
    }

    @Test
    @SuppressWarnings("unchecked")
    void listar_comoAdministrador_comOsFiltrosDaTelaDeLogs_deveRetornarAEntrada() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Long turmaId = criarTurma(administrador);

        // Act: mesma URL montada por LogsAdministrador.tsx
        ResponseEntity<Map<String, Object>> resposta = client.get()
                .uri(BASE_PATH + "?page=0&size=20&acao=CRIAR_TURMA&entidade=TURMA&resultado=SUCESSO&usuarioId={id}",
                        administrador.id())
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> entradas = (List<Map<String, Object>>) resposta.getBody().get("content");
        assertThat(entradas).hasSize(1);
        assertThat(((Number) entradas.get(0).get("entidadeId")).longValue()).isEqualTo(turmaId);
    }

    @Test
    void preflightCors_semToken_naoDeveSerBloqueadoPelaRestricaoDeAdministrador() {
        // Act: sem isso o navegador nem chegaria a enviar o GET da tela de logs
        ResponseEntity<Void> resposta = client.method(HttpMethod.OPTIONS).uri(BASE_PATH)
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}

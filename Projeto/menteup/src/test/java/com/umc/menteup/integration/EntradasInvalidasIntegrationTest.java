package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.security.JwtService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/*
 * Entradas inválidas devem dar 400/401/404 (erro de quem chamou), e não 500 (erro do sistema).
 */
class EntradasInvalidasIntegrationTest extends IntegracaoBase {

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private JwtService jwtService;

    // ------------------------------------------------------------- Parâmetros com tipo errado

    @Test
    void idComTexto_deveRetornar400SemRegistrarErroDeSistema() {
        Sessao professor = novaSessao("PROFESSOR");
        long errosAntes = contarErrosDeSistema();

        ResponseEntity<Map<String, Object>> resposta = get(professor.token(), "/turmas/abc");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "PARAMETRO_INVALIDO");
        assertThat(contarErrosDeSistema()).isEqualTo(errosAntes);
    }

    @Test
    void enumInvalidoNosLogs_deveRetornar400() {
        Sessao administrador = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = get(administrador.token(), "/logs?acao=XPTO");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "PARAMETRO_INVALIDO");
    }

    // ------------------------------------------------------------- Serviços

    @Test
    void questionarioDeAtividadeInexistente_deveRetornar404() {
        Sessao professor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> resposta = client.post()
                .uri("/atividades/{id}/questionario", Long.MAX_VALUE)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntas", List.of(Map.of(
                        "enunciado", "Pergunta",
                        "alternativas", List.of(
                                Map.of("texto", "A", "correta", true),
                                Map.of("texto", "B", "correta", false))))))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENCONTRADA");
    }

    // ------------------------------------------------------------- Tokens inválidos -> 401

    @Test
    void tokenMalformado_deveRetornar401() {
        assertTokenInvalido("Bearer isto-nao-e-um-jwt");
    }

    @Test
    void tokenSemAssinatura_deveRetornar401() {
        // JWT "sem assinatura" (UnsupportedJwtException): antes virava 500
        String semAssinatura = Jwts.builder()
                .subject("alguem@teste.com")
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .compact();

        assertTokenInvalido("Bearer " + semAssinatura);
    }

    @Test
    void tokenAssinadoComOutraChave_deveRetornar401() {
        Sessao aluno = novaSessao("ALUNO");
        SecretKey outraChave = Keys.hmacShaKeyFor(Base64.getDecoder().decode(
                "c3VhLWNoYXZlLWZhbHNhLWNvbS1tYWlzLWRlLTMyLWJ5dGVzLXBhcmEtbyB0ZXN0ZQ=="));
        String falso = Jwts.builder()
                .subject(aluno.email())
                .expiration(Date.from(Instant.now().plusSeconds(600)))
                .signWith(outraChave)
                .compact();

        assertTokenInvalido("Bearer " + falso);
    }

    @Test
    void tokenDeUsuarioQueNaoExiste_deveRetornar401() {
        // Assinado com a chave certa, mas o e-mail não está cadastrado (ex.: conta excluída)
        String token = jwtService.generateToken(emailUnico("conta.excluida"));

        assertTokenInvalido("Bearer " + token);
    }

    @Test
    void erroDentroDoControllerComTokenValido_naoDeveVirar401() {
        // O filtro só trata o token: um 404 do controller continua 404
        Sessao professor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> resposta = get(professor.token(), "/turmas/" + Long.MAX_VALUE);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ------------------------------------------------------------- Helpers

    private void assertTokenInvalido(String authorization) {
        ResponseEntity<Map<String, Object>> resposta = get(authorization, "/turmas/all");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("codigo", "TOKEN_INVALIDO");
    }

    private ResponseEntity<Map<String, Object>> get(String authorization, String uri) {
        return client.get().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .retrieve()
                .toEntity(JSON);
    }

    private long contarErrosDeSistema() {
        return logSistemaRepository.findAll().stream()
                .filter(log -> log.getAcao() == AcaoLog.ERRO_SISTEMA)
                .count();
    }
}

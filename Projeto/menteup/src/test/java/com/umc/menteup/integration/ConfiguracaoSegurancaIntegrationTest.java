package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// CORS só para os endereços configurados e documentação da API desligada por padrão
class ConfiguracaoSegurancaIntegrationTest extends IntegracaoBase {

    @Test
    void preflight_deOrigemNaoConfigurada_deveSerRecusado() {
        // Qualquer site hospedado no Vercel era aceito antes; agora só o que está em CORS_ORIGENS
        ResponseEntity<Void> resposta = preflight("https://site-qualquer.vercel.app");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void preflight_deOrigemConfigurada_deveSerLiberado() {
        ResponseEntity<Void> resposta = preflight("http://localhost:5173");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:5173");
    }

    @Test
    void rotaQueTinhaCrossOriginLiberado_naoDeveAceitarQualquerOrigem() {
        // /usuarios/senha/esqueci tinha @CrossOrigin("*") no controller
        ResponseEntity<Void> resposta = client.method(HttpMethod.OPTIONS).uri("/usuarios/senha/esqueci")
                .header(HttpHeaders.ORIGIN, "https://site-qualquer.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getHeaders().getAccessControlAllowOrigin()).isNull();
    }

    @Test
    void documentacaoDaApi_deveFicarDesligadaPorPadrao() {
        ResponseEntity<Void> resposta = client.get().uri("/v3/api-docs")
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private ResponseEntity<Void> preflight(String origem) {
        return client.method(HttpMethod.OPTIONS).uri("/turmas")
                .header(HttpHeaders.ORIGIN, origem)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .retrieve()
                .toBodilessEntity();
    }
}

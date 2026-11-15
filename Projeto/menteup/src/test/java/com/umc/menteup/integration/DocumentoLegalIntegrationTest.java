package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.service.DocumentoLegalService;

// Termo de Aceite e Política de Privacidade servidos pelo back-end, sem login
class DocumentoLegalIntegrationTest extends IntegracaoBase {

    @Autowired
    private DocumentoLegalService documentoLegalService;

    @Test
    void atual_semToken_deveTrazerOTextoDaVersaoVigente() {
        ResponseEntity<Map<String, Object>> termo = client.get().uri("/documentos/termo/atual")
                .retrieve()
                .toEntity(JSON);
        ResponseEntity<Map<String, Object>> politica = client.get().uri("/documentos/politica/atual")
                .retrieve()
                .toEntity(JSON);

        assertThat(termo.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(termo.getBody())
                .containsEntry("tipo", "TERMO")
                // A vigente muda conforme a data: 1.0 até a 1.1 entrar em vigor
                .containsEntry("versao", documentoLegalService.vigente(TipoDocumento.TERMO).getVersao())
                .containsKey("vigenteDesde");
        assertThat((String) termo.getBody().get("conteudo")).startsWith("## 1. Apresentação");

        assertThat(politica.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(politica.getBody()).containsEntry("tipo", "POLITICA");
        // O cabeçalho do arquivo (entre ---) não vai para o texto
        assertThat((String) politica.getBody().get("conteudo")).doesNotContain("vigenteDesde:");
    }

    @Test
    void versoes_deveListarOHistoricoSemOTexto() {
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri("/documentos/termo/versoes")
                .retrieve()
                .toEntity(LISTA_JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isNotEmpty()
                .allSatisfy(v -> assertThat(v).containsKeys("versao", "vigenteDesde", "exigeNovoAceite")
                        .doesNotContainKey("conteudo"));
    }

    @Test
    void versaoEspecifica_deveTrazerOTextoDaquelaVersao() {
        ResponseEntity<Map<String, Object>> resposta = client.get()
                .uri("/documentos/politica/versoes/{versao}", "1.0")
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("versao", "1.0").containsKey("conteudo");
    }

    @Test
    void tipoOuVersaoInexistente_deveRetornar404() {
        assertThat(status("/documentos/contrato/atual")).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(status("/documentos/termo/versoes/0.1")).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void compararVersoes_deveOrdenarComoNumeros() {
        assertThat(DocumentoLegal.compararVersoes("1.10", "1.9")).isPositive();
        assertThat(DocumentoLegal.compararVersoes("2.0", "10.0")).isNegative();
        assertThat(DocumentoLegal.compararVersoes("1.0", "1")).isZero();
    }

    private HttpStatus status(String uri) {
        return HttpStatus.valueOf(client.get().uri(uri)
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value());
    }
}

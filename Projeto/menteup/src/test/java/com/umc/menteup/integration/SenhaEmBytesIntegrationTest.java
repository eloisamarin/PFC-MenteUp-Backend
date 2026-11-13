package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/*
 * O BCrypt só usa os primeiros 72 BYTES da senha. Letra com acento ocupa 2 bytes,
 * então a regra é conferida em bytes, além do limite de 72 caracteres dos DTOs.
 */
class SenhaEmBytesIntegrationTest extends IntegracaoBase {

    // 40 caracteres (passa no @Size), mas 80 bytes
    private static final String SENHA_COM_ACENTOS_LONGA = "á".repeat(40);
    // 38 caracteres e exatamente 72 bytes (4 de "Aa1!" + 34 "ç" de 2 bytes): ainda vale
    private static final String SENHA_NO_LIMITE = "Aa1!" + "ç".repeat(34);

    @Test
    void senhasDoTeste_temOsTamanhosEsperados() {
        assertThat(SENHA_COM_ACENTOS_LONGA).hasSize(40);
        assertThat(SENHA_COM_ACENTOS_LONGA.getBytes(StandardCharsets.UTF_8)).hasSize(80);
        assertThat(SENHA_NO_LIMITE.getBytes(StandardCharsets.UTF_8)).hasSize(72);
    }

    @Test
    void cadastro_comSenhaAcimaDe72Bytes_deveRetornar400ENaoCriar() {
        String email = emailUnico("acentos");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, SENHA_COM_ACENTOS_LONGA);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_LONGA_DEMAIS");
        assertThat(logar(email, SENHA_COM_ACENTOS_LONGA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cadastro_comSenhaDeExatamente72Bytes_deveCriarEEntrar() {
        String email = emailUnico("limite");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, SENHA_NO_LIMITE);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(logar(email, SENHA_NO_LIMITE)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void alterarSenha_comNovaSenhaAcimaDe72Bytes_deveRetornar400EManterASenhaAtual() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", SENHA, "novaSenha", SENHA_COM_ACENTOS_LONGA))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_LONGA_DEMAIS");
        assertThat(logar(aluno.email(), SENHA)).isEqualTo(HttpStatus.OK);
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> cadastrar(String email, String senha) {
        Map<String, Object> dados = new HashMap<>(buildUsuario("Pessoa Teste", email, "ALUNO"));
        dados.put("senha", senha);
        return client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);
    }

    private HttpStatus logar(String email, String senha) {
        return HttpStatus.valueOf(client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", senha))
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value());
    }
}

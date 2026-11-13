package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/*
 * Senha forte nas rotas (RF03 a RF06 e RF09): a API recusa a senha fraca mesmo que
 * alguém chame a rota direto, sem passar pela tela.
 */
class SenhaForteIntegrationTest extends IntegracaoBase {

    @Test
    void cadastro_comSenhaFraca_deveRetornar400ENaoCriar() {
        String email = emailUnico("fraca");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, "senha1234");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_FRACA");
        assertThat((String) resposta.getBody().get("mensagem"))
                .contains("uma letra maiúscula", "um caractere especial");
        assertThat(logar(email, "senha1234")).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cadastro_comEspacoNoFim_deveRetornar400() {
        ResponseEntity<Map<String, Object>> resposta = cadastrar(emailUnico("espaco"), "Senha@123 ");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_COM_ESPACO");
    }

    @Test
    void cadastro_comSenhaForte_deveCriarEEntrar() {
        String email = emailUnico("forte");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, "Senha@123");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(logar(email, "Senha@123")).isEqualTo(HttpStatus.OK);
    }

    @Test
    void trocarSenhaNoPerfil_paraSenhaFraca_deveRetornar400EManterAAtual() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", SENHA, "novaSenha", "semnumero!"))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_FRACA");
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

package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;

/*
 * Validação dos campos: dados inválidos voltam 400 VALIDACAO_FALHOU com o campo e a mensagem,
 * em vez de chegar no banco (409 confuso, 500) ou passar sem controle.
 */
class ValidacaoCamposIntegrationTest extends IntegracaoBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ------------------------------------------------------------- Login e 2FA

    @Test
    void login_comEmailInvalidoESemSenha_deveRetornar400() {
        ResponseEntity<Map<String, Object>> resposta = post("/usuarios/logar", null,
                Map.of("usuario", "nao-e-email", "senha", ""));

        assertCampos(resposta, "usuario", "senha");
    }

    @Test
    void validar2FA_comCodigoQueNaoTem6Digitos_deveRetornar400() {
        ResponseEntity<Map<String, Object>> resposta = post("/usuarios/validar-2fa", null,
                Map.of("usuario", emailUnico("x"), "codigo", "12ab"));

        assertCampos(resposta, "codigo");
    }

    @Test
    void reenviar2FA_semEmail_deveRetornar400() {
        ResponseEntity<Map<String, Object>> resposta = post("/usuarios/reenviar-2fa", null, Map.of());

        assertCampos(resposta, "usuario");
    }

    // ------------------------------------------------------------- Usuário

    @Test
    void cadastro_comNomeLongoESenhaAcimaDe72_deveRetornar400() {
        Map<String, Object> dados = new HashMap<>(buildUsuario("N".repeat(101), emailUnico("longo"), "ALUNO"));
        dados.put("senha", "s".repeat(73));

        ResponseEntity<Map<String, Object>> resposta = post("/usuarios/cadastrar", null, dados);

        assertCampos(resposta, "nomeUsuario", "senha");
    }

    @Test
    void atualizar_naoDeveApagarTokenDoCelularNemExigirSenha() {
        // Arrange: o aluno já tem token do celular gravado
        Sessao aluno = novaSessao("ALUNO");
        Usuario antes = usuarioRepository.findById(aluno.id()).orElseThrow();
        antes.setFcmToken("token-do-celular");
        usuarioRepository.save(antes);

        // Act: só nome, e-mail e tipo (sem senha)
        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("id", aluno.id(), "nomeUsuario", "Nome Novo",
                        "usuario", aluno.email(), "tipoUsuario", "ALUNO"))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Usuario depois = usuarioRepository.findById(aluno.id()).orElseThrow();
        assertThat(depois.getNomeUsuario()).isEqualTo("Nome Novo");
        assertThat(depois.getFcmToken()).isEqualTo("token-do-celular");
        assertThat(autenticar(aluno.email())).isNotBlank(); // a senha continua a mesma
    }

    // ------------------------------------------------------------- Atividade e questionário

    @Test
    void atividade_comPontuacaoAcimaDe1000_deveRetornar400() {
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> dados = new HashMap<>(buildAtividade(nomeUnico("Atividade"), criarTurma(professor), "PENDENTE"));
        dados.put("pontuacao", 1_000_000);

        ResponseEntity<Map<String, Object>> resposta = post("/atividades", professor.token(), dados);

        assertCampos(resposta, "pontuacao");
    }

    @Test
    void questionario_comTextoAcimaDe255_deveRetornar400ENao409() {
        Sessao professor = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "PENDENTE");
        Map<String, Object> pergunta = Map.of(
                "enunciado", "E".repeat(256),
                "alternativas", List.of(
                        Map.of("texto", "A".repeat(256), "correta", true),
                        Map.of("texto", "B", "correta", false)));

        ResponseEntity<Map<String, Object>> resposta = post(
                "/atividades/" + atividadeId + "/questionario", professor.token(), Map.of("perguntas", List.of(pergunta)));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> post(String uri, String token, Object corpo) {
        var requisicao = client.post().uri(uri).contentType(MediaType.APPLICATION_JSON);
        if (token != null) requisicao = requisicao.header(HttpHeaders.AUTHORIZATION, token);
        return requisicao.body(corpo).retrieve().toEntity(JSON);
    }

    @SuppressWarnings("unchecked")
    private static void assertCampos(ResponseEntity<Map<String, Object>> resposta, String... campos) {
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos")).containsKeys(campos);
    }
}

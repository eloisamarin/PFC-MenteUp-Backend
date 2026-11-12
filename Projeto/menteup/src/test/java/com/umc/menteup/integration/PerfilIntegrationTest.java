package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;


public class PerfilIntegrationTest extends IntegracaoBase {
    private static final String NOVA_SENHA = "nova-senha-123";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    // ------------------------------------------------------------ GET /usuarios/me

    @Test
    @SuppressWarnings("unchecked")
    void buscarPerfil_comoAluno_deveRetornarNomeEmailAnonimizadoETurmas() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarPerfil(aluno);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("nome", "Usuário Teste")
                .containsEntry("tipoUsuario", "ALUNO")
                .containsEntry("emailAnonimizado", "al***@teste.com");

        List<Map<String, Object>> turmas = (List<Map<String, Object>>) resposta.getBody().get("turmas");
        assertThat(turmas).extracting(IntegracaoBase::id).containsExactly(turmaId);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPerfil_comoProfessor_deveListarAsTurmasQueLeciona() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarPerfil(professor);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("tipoUsuario", "PROFESSOR");
        assertThat((List<Map<String, Object>>) resposta.getBody().get("turmas"))
                .extracting(IntegracaoBase::id)
                .containsExactly(turmaId);
    }

    @Test
    void buscarPerfil_naoDeveExporOEmailCompletoNemASenha() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<String> resposta = client.get().uri("/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(String.class);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .doesNotContain(aluno.email())
                .doesNotContain("senha")
                .doesNotContain(SENHA);
    }

    @Test
    void buscarPerfil_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri("/usuarios/me")
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------ PUT /usuarios/me/senha

    @Test
    void alterarSenha_comSenhaAtualCorreta_deveRetornar204ETrocarOLogin() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = alterarSenha(aluno, SENHA, NOVA_SENHA);

        // Assert: a nova senha entra e a antiga não
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logar(aluno.email(), NOVA_SENHA)).isEqualTo(HttpStatus.OK);
        assertThat(logar(aluno.email(), SENHA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void alterarSenha_comSenhaAtualErrada_deveRetornar400ENaoTrocar() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = alterarSenha(aluno, "senha-errada-123", NOVA_SENHA);

        // Assert: 400 (e não 401), para o frontend não deslogar a pessoa
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_ATUAL_INCORRETA");
        assertThat(logar(aluno.email(), SENHA)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void alterarSenha_comNovaSenhaIgualAAtual_deveRetornar400ComCodigo() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = alterarSenha(aluno, SENHA, SENHA);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "NOVA_SENHA_IGUAL");
    }

    @Test
    @SuppressWarnings("unchecked")
    void alterarSenha_comNovaSenhaCurta_deveRetornar400ComCampoInvalido() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = alterarSenha(aluno, SENHA, "123");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos"))
                .containsEntry("novaSenha", "A nova senha deve ter de 8 a 72 caracteres");
    }

    // ------------------------------------------------- POST /usuarios/me/exclusao

    @Test
    void excluirConta_comSenhaCorreta_deveAnonimizarETirarDasTurmas() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluirConta(aluno, SENHA);

        // Assert: os dados pessoais foram trocados e o aluno saiu da turma
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        Usuario anonimizado = usuarioRepository.findById(aluno.id()).orElseThrow();
        assertThat(anonimizado.getNomeUsuario()).isEqualTo("Usuário removido");
        assertThat(anonimizado.getUsuario()).isEqualTo("removido-" + aluno.id() + "@menteup.invalid");
        assertThat(anonimizado.getFcmToken()).isNull();
        assertThat(turmaRepository.contarMatricula(turmaId, aluno.id())).isZero();
        assertThat(codigo2FARepository.findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(aluno.email())).isEmpty();

        // Assert: o token antigo e o login antigo deixam de funcionar
        assertThat(buscarPerfil(aluno).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(logar(aluno.email(), SENHA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void excluirConta_comSenhaErrada_deveRetornar400ENaoAlterarNada() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluirConta(aluno, "senha-errada-123");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SENHA_INCORRETA");
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getUsuario()).isEqualTo(aluno.email());
    }

    @Test
    void excluirConta_comoAdministrador_deveRetornar403ENaoAlterar() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluirConta(administrador, SENHA);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ADMINISTRADOR_NAO_PODE_EXCLUIR");
        assertThat(usuarioRepository.findById(administrador.id()).orElseThrow().getUsuario())
                .isEqualTo(administrador.email());
    }

    @Test
    void excluirConta_semSenha_deveRetornar400ComCodigo() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluirConta(aluno, " ");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }
    private ResponseEntity<Map<String, Object>> alterarAvatar(Sessao sessao, String avatar) {
        return client.put().uri("/usuarios/me/avatar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("avatar", avatar))
                .retrieve()
                .toEntity(JSON);
    }

    // ------------------------------------------------------------------ Helpers

    private ResponseEntity<Map<String, Object>> buscarPerfil(Sessao sessao) {
        return client.get().uri("/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> alterarSenha(Sessao sessao, String senhaAtual, String novaSenha) {
        return client.put().uri("/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", senhaAtual, "novaSenha", novaSenha))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> excluirConta(Sessao sessao, String senha) {
        return client.post().uri("/usuarios/me/exclusao")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senha", senha))
                .retrieve()
                .toEntity(JSON);
    }

    /** Primeiro passo do login (usuário e senha); devolve só o status. */
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

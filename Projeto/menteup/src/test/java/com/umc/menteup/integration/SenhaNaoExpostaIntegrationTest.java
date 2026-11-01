package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.umc.menteup.repository.UsuarioRepository;

/**
 * Lê o corpo cru de cada resposta: assim a senha é detectada em qualquer nível do JSON,
 * inclusive nos usuários aninhados (alunos da turma).
 */
class SenhaNaoExpostaIntegrationTest extends IntegracaoBase {

    private static final String REGEX_HASH_BCRYPT = ".*\\$2[aby]\\$\\d{2}\\$.*";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private ResponseEntity<String> buscar(Sessao sessao, String uri, Object... parametros) {
        return client.get().uri(uri, parametros)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(String.class);
    }

    private static void assertSemSenha(ResponseEntity<String> resposta) {
        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        // Com aspas: "senha" solto também casaria com e-mails de outros testes do mesmo banco
        assertThat(resposta.getBody())
                .isNotBlank()
                .doesNotContain("\"senha\"")
                .doesNotContainPattern(REGEX_HASH_BCRYPT);
    }

    @Test
    void cadastrar_naoDeveDevolverSenhaMasDeveGravarOHash() {
        // Arrange
        String email = emailUnico("cadastro.sem.hash");

        // Act
        ResponseEntity<String> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Usuário Teste", email, "ALUNO"))
                .retrieve()
                .toEntity(String.class);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertSemSenha(resposta);
        String hash = usuarioRepository.findByUsuario(email).orElseThrow().getSenha();
        assertThat(passwordEncoder.matches(SENHA, hash)).isTrue();
    }

    @Test
    void atualizar_deveAceitarSenhaNaEntradaESemDevolverNaResposta() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        String novaSenha = "nova-" + SENHA;
        Map<String, Object> alteracao = buildUsuario("Nome Atualizado", aluno.email(), "ALUNO");
        alteracao.put("id", aluno.id());
        alteracao.put("senha", novaSenha);

        // Act
        ResponseEntity<String> resposta = client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(alteracao)
                .retrieve()
                .toEntity(String.class);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertSemSenha(resposta);
        String hash = usuarioRepository.findById(aluno.id()).orElseThrow().getSenha();
        assertThat(passwordEncoder.matches(novaSenha, hash)).isTrue();
    }

    @Test
    void buscarUsuarios_comoAluno_naoDeveDevolverSenha() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");

        // Act + Assert
        assertSemSenha(buscar(aluno, "/usuarios/all"));
        assertSemSenha(buscar(aluno, "/usuarios/{id}", aluno.id()));
    }

    @Test
    void buscarTurmas_comoAluno_naoDeveDevolverSenhaDosAlunos() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        ResponseEntity<String> porId = buscar(aluno, "/turmas/{id}", turmaId);

        // Assert: a turma precisa trazer o aluno, senão o teste não exercita o campo alunos
        assertThat(porId.getBody()).contains(aluno.email());
        assertSemSenha(porId);
        assertSemSenha(buscar(aluno, "/turmas/all"));
    }
}

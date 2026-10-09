package com.umc.menteup.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.umc.menteup.exception.ErroResponse;
import com.umc.menteup.model.Turma;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.EmailService;
import com.umc.menteup.service.UsuarioService;
import com.umc.menteup.util.JwtHelper;
import com.umc.menteup.util.TestBuilder;

/**
 * Teste de integração do TurmaController.
 * Sobe a aplicação inteira (controller + segurança JWT + service + repositório)
 * com banco H2 em memória e valida a regra: somente PROFESSOR e ADMINISTRADOR
 * criam, alteram e excluem turmas e vinculam alunos a elas.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class TurmaControllerTest{

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private Codigo2FARepository codigo2FARepository;

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @MockitoBean
    private EmailService emailService; // evita o envio real de e-mail do 2FA

    private static final String BASE_URL = "/turmas";

    private static final String SENHA = "12345678";
    private static final String EMAIL_PROFESSOR = "professor@teste.com";
    private static final String EMAIL_ADMIN = "admin@teste.com";
    private static final String EMAIL_ALUNO = "aluno@teste.com";

    private String tokenProfessor;
    private String tokenAdmin;
    private String tokenAluno;
    private Long alunoId;

    @BeforeAll
    void start() {
        turmaRepository.deleteAll();
        codigo2FARepository.deleteAll();
        logSistemaRepository.deleteAll(); // logs referenciam usuários (FK)
        usuarioRepository.deleteAll();

        usuarioService.cadastrarUsuario(
                TestBuilder.criarUsuario(null, "Professor", EMAIL_PROFESSOR, SENHA, "PROFESSOR"));
        usuarioService.cadastrarUsuario(
                TestBuilder.criarUsuario(null, "Administrador", EMAIL_ADMIN, SENHA, "ADMINISTRADOR"));
        alunoId = usuarioService.cadastrarUsuario(
                TestBuilder.criarUsuario(null, "Aluno", EMAIL_ALUNO, SENHA, "ALUNO")).get().getId();

        tokenProfessor = JwtHelper.obterToken(testRestTemplate, codigo2FARepository, EMAIL_PROFESSOR, SENHA);
        tokenAdmin = JwtHelper.obterToken(testRestTemplate, codigo2FARepository, EMAIL_ADMIN, SENHA);
        tokenAluno = JwtHelper.obterToken(testRestTemplate, codigo2FARepository, EMAIL_ALUNO, SENHA);
    }

    // Cada teste começa sem nenhuma turma no banco
    @BeforeEach
    void limparTurmas() {
        turmaRepository.deleteAll();
    }

    // ------------------------------------------------------------------
    // Criar turma
    // ------------------------------------------------------------------

    @Test
    @DisplayName("✔ 01 - Professor deve criar uma turma com sucesso")
    void professorDeveCriarTurma() {

        // Given
        Turma turma = novaTurma("Turma de Java");

        // When
        HttpEntity<Turma> requisicao = JwtHelper.criarRequisicaoComToken(turma, tokenProfessor);
        ResponseEntity<Turma> resposta = testRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, requisicao, Turma.class);

        // Then
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertNotNull(resposta.getBody().getId());
        assertEquals("Turma de Java", resposta.getBody().getNome());
        assertTrue(turmaRepository.findById(resposta.getBody().getId()).isPresent());
    }

    @Test
    @DisplayName("✔ 02 - Administrador deve criar uma turma com sucesso")
    void administradorDeveCriarTurma() {

        // Given
        Turma turma = novaTurma("Turma do Admin");

        // When
        HttpEntity<Turma> requisicao = JwtHelper.criarRequisicaoComToken(turma, tokenAdmin);
        ResponseEntity<Turma> resposta = testRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, requisicao, Turma.class);

        // Then
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertEquals(1, turmaRepository.count());
    }

    @Test
    @DisplayName("✔ 03 - Aluno não deve criar turma")
    void alunoNaoDeveCriarTurma() {

        // Given
        Turma turma = novaTurma("Turma Proibida");

        // When
        HttpEntity<Turma> requisicao = JwtHelper.criarRequisicaoComToken(turma, tokenAluno);
        ResponseEntity<ErroResponse> resposta = testRestTemplate.exchange(
                BASE_URL, HttpMethod.POST, requisicao, ErroResponse.class);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals("SOMENTE_PROFESSOR_OU_ADMINISTRADOR", resposta.getBody().codigo());
        assertEquals(0, turmaRepository.count());
    }

    @Test
    @DisplayName("✔ 04 - Requisição sem token deve ser rejeitada")
    void semTokenDeveRetornar401() {

        // When
        ResponseEntity<String> resposta = testRestTemplate.exchange(
                BASE_URL + "/all", HttpMethod.GET, null, String.class);

        // Then
        assertEquals(HttpStatus.UNAUTHORIZED, resposta.getStatusCode());
    }

    // ------------------------------------------------------------------
    // Vincular aluno
    // ------------------------------------------------------------------

    @Test
    @DisplayName("✔ 05 - Professor deve vincular um aluno à turma")
    void professorDeveVincularAluno() {

        // Given
        Long turmaId = salvarTurma("Turma de Spring");

        // When
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(tokenProfessor);
        ResponseEntity<Void> resposta = testRestTemplate.exchange(
                BASE_URL + "/" + turmaId + "/alunos/" + alunoId, HttpMethod.POST, requisicao, Void.class);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        assertEquals(1, turmaRepository.contarMatricula(turmaId, alunoId));
    }

    @Test
    @DisplayName("✔ 06 - Aluno não deve vincular alunos à turma")
    void alunoNaoDeveVincularAluno() {

        // Given
        Long turmaId = salvarTurma("Turma de Spring");

        // When
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(tokenAluno);
        ResponseEntity<ErroResponse> resposta = testRestTemplate.exchange(
                BASE_URL + "/" + turmaId + "/alunos/" + alunoId, HttpMethod.POST, requisicao, ErroResponse.class);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertEquals(0, turmaRepository.contarMatricula(turmaId, alunoId));
    }

    @Test
    @DisplayName("✔ 07 - Não deve vincular o mesmo aluno duas vezes")
    void naoDeveVincularAlunoDuasVezes() {

        // Given
        Long turmaId = salvarTurma("Turma de Spring");
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(tokenProfessor);
        String url = BASE_URL + "/" + turmaId + "/alunos/" + alunoId;
        testRestTemplate.exchange(url, HttpMethod.POST, requisicao, Void.class);

        // When
        ResponseEntity<ErroResponse> resposta = testRestTemplate.exchange(
                url, HttpMethod.POST, requisicao, ErroResponse.class);

        // Then
        assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
        assertEquals(1, turmaRepository.contarMatricula(turmaId, alunoId));
    }

    // ------------------------------------------------------------------
    // Atualizar e excluir
    // ------------------------------------------------------------------

    @Test
    @DisplayName("✔ 08 - Professor deve atualizar o nome sem perder os alunos")
    void professorDeveAtualizarNomeMantendoAlunos() {

        // Given: turma com um aluno vinculado
        Long turmaId = salvarTurma("Nome Antigo");
        testRestTemplate.exchange(BASE_URL + "/" + turmaId + "/alunos/" + alunoId, HttpMethod.POST,
                JwtHelper.criarRequisicaoComToken(tokenProfessor), Void.class);

        Turma dados = novaTurma("Nome Novo");
        dados.setId(turmaId);

        // When
        HttpEntity<Turma> requisicao = JwtHelper.criarRequisicaoComToken(dados, tokenProfessor);
        ResponseEntity<Void> resposta = testRestTemplate.exchange(
                BASE_URL, HttpMethod.PUT, requisicao, Void.class);

        // Then
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertEquals("Nome Novo", turmaRepository.findById(turmaId).get().getNome());
        assertEquals(1, turmaRepository.contarMatricula(turmaId, alunoId));
    }

    @Test
    @DisplayName("✔ 09 - Aluno não deve excluir turma")
    void alunoNaoDeveExcluirTurma() {

        // Given
        Long turmaId = salvarTurma("Turma Protegida");

        // When
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(tokenAluno);
        ResponseEntity<ErroResponse> resposta = testRestTemplate.exchange(
                BASE_URL + "/" + turmaId, HttpMethod.DELETE, requisicao, ErroResponse.class);

        // Then
        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertTrue(turmaRepository.findById(turmaId).isPresent());
    }

    @Test
    @DisplayName("✔ 10 - Administrador deve excluir uma turma")
    void administradorDeveExcluirTurma() {

        // Given
        Long turmaId = salvarTurma("Turma Para Excluir");

        // When
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(tokenAdmin);
        ResponseEntity<Void> resposta = testRestTemplate.exchange(
                BASE_URL + "/" + turmaId, HttpMethod.DELETE, requisicao, Void.class);

        // Then
        assertEquals(HttpStatus.NO_CONTENT, resposta.getStatusCode());
        assertFalse(turmaRepository.findById(turmaId).isPresent());
    }

    // ------------------------------------------------------------------
    // Auxiliares
    // ------------------------------------------------------------------

    private Turma novaTurma(String nome) {
        Turma turma = new Turma();
        turma.setNome(nome);
        return turma;
    }

    // Grava a turma direto no banco (preparação do cenário "Given")
    private Long salvarTurma(String nome) {
        return turmaRepository.save(novaTurma(nome)).getId();
    }
}


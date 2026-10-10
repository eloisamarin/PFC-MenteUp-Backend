package com.umc.menteup.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
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

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.service.EmailService;
import com.umc.menteup.service.UsuarioService;
import com.umc.menteup.util.JwtHelper;
import com.umc.menteup.util.TestBuilder;



@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT) //A aplicação inteira numa porta livre
@AutoConfigureTestRestTemplate // libera o cliente HTTP
@ActiveProfiles("test") // Desliga o Firebase
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //troca o serviço de e-mail por um falso, para nenhum e-mail real ser enviado
@TestMethodOrder(MethodOrderer.DisplayName.class)
public class UsuarioControllerTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private Codigo2FARepository codigo2FARepository;

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @MockitoBean
    private EmailService emailService; // evita o envio real de e-mail

    private static final String BASE_URL = "/usuarios";
    private static final String USUARIO_ADMIN = "root@root.com";
    private static final String SENHA_ADMIN = "rootroot";

    private String token;

    @BeforeAll
    void start() {
        codigo2FARepository.deleteAll();
        logSistemaRepository.deleteAll(); // logs referenciam usuários (FK)
        usuarioRepository.deleteAll();

        usuarioService.cadastrarUsuario(
                TestBuilder.criarUsuario(null, "Root", USUARIO_ADMIN, SENHA_ADMIN, "ADMINISTRADOR"));

        token = JwtHelper.obterToken(testRestTemplate, codigo2FARepository, USUARIO_ADMIN, SENHA_ADMIN);
    }

    @Test
    @DisplayName("✔ 01 - Deve cadastrar um novo usuário com sucesso")
    void deveCadastrarUsuario() {

        // Given
        Usuario usuario = TestBuilder.criarUsuario(null, "Paulo Antunes", "paulo_antunes@email.com.br", "12345678");

        // When
        HttpEntity<Usuario> requisicao = new HttpEntity<>(usuario);
        ResponseEntity<Usuario> resposta = testRestTemplate.exchange(
                BASE_URL + "/cadastrar", HttpMethod.POST, requisicao, Usuario.class);

        // Then
        assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertNotNull(resposta.getBody().getId());
        assertEquals("Paulo Antunes", resposta.getBody().getNomeUsuario());
        assertEquals("paulo_antunes@email.com.br", resposta.getBody().getUsuario());
        assertTrue(usuarioRepository.findByUsuario("paulo_antunes@email.com.br").isPresent());
    }

    @Test
    @DisplayName("✔ 02 - Não deve permitir a duplicação do usuário")
    void naoDeveDuplicarUsuario() {

        // Given
        Usuario usuario = TestBuilder.criarUsuario(null, "Maria da Silva", "maria_silva@email.com.br", "12345678");
        usuarioService.cadastrarUsuario(usuario);

        // When
        HttpEntity<Usuario> requisicao = new HttpEntity<>(usuario);
        ResponseEntity<Usuario> resposta = testRestTemplate.exchange(
                BASE_URL + "/cadastrar", HttpMethod.POST, requisicao, Usuario.class);

        // Then
        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertNull(resposta.getBody());
    }

    @Test
    @DisplayName("✔ 03 - Deve atualizar os dados de um usuário com sucesso")
    void deveAtualizarUmUsuario() {

        // Given
        Usuario usuario = TestBuilder.criarUsuario(null, "Juliana Andrews", "ju_andrews@email.com.br", "12345678");
        Optional<Usuario> cadastrado = usuarioService.cadastrarUsuario(usuario);

        Usuario usuarioUpdate = TestBuilder.criarUsuario(cadastrado.get().getId(), "Juliana Ramos",
                "ju_ramos@email.com.br", "12345678");

        // When
        HttpEntity<Usuario> requisicao = JwtHelper.criarRequisicaoComToken(usuarioUpdate, token);
        ResponseEntity<Usuario> resposta = testRestTemplate.exchange(
                BASE_URL + "/atualizar", HttpMethod.PUT, requisicao, Usuario.class);

        // Then
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        assertEquals(cadastrado.get().getId(), resposta.getBody().getId());
        assertEquals("Juliana Ramos", resposta.getBody().getNomeUsuario());
        assertEquals("ju_ramos@email.com.br", resposta.getBody().getUsuario());
    }

    @Test
    @DisplayName("✔ 04 - Deve listar todos os usuários com sucesso")
    void deveListarTodosUsuarios() {

        // Given
        usuarioService.cadastrarUsuario(TestBuilder.criarUsuario(null, "Ana Marques",
                "ana_marques@email.com.br", "12345678"));
        usuarioService.cadastrarUsuario(TestBuilder.criarUsuario(null, "Carlos Moura",
                "carlos_moura@email.com.br", "12345678"));

        // When
        HttpEntity<Void> requisicao = JwtHelper.criarRequisicaoComToken(token);
        ResponseEntity<Usuario[]> resposta = testRestTemplate.exchange(
                BASE_URL + "/all", HttpMethod.GET, requisicao, Usuario[].class);

        // Then
        assertEquals(HttpStatus.OK, resposta.getStatusCode());
        assertNotNull(resposta.getBody());
        List<String> emails = Arrays.stream(resposta.getBody()).map(Usuario::getUsuario).toList();
        assertTrue(emails.contains("ana_marques@email.com.br"));
        assertTrue(emails.contains("carlos_moura@email.com.br"));
    }
}
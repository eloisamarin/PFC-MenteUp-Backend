package com.umc.menteup.integration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.LocalDateTime;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.TokenRedefinicaoSenha;
import com.umc.menteup.repository.TokenRedefinicaoSenhaRepository;
import com.umc.menteup.repository.UsuarioRepository;

/*
 - Testes de integração do "esqueci minha senha" e do bloqueio após 3 senhas erradas.
 - O EmailService é mockado (IntegracaoBase): o link é capturado do mock em vez de chegar por e-mail.
 */
class RedefinicaoSenhaIntegrationTest extends IntegracaoBase {

    private static final String NOVA_SENHA = "Nova-Senha-123";
    private static final String SENHA_ERRADA = "senha-errada-123";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TokenRedefinicaoSenhaRepository tokenRepository;

    // ------------------------------------------------- POST /usuarios/senha/esqueci

    @Test
    void esqueciSenha_comEmailCadastrado_deveEnviarLinkQueTrocaASenha() {
        // Arrange
        String email = emailUnico("aluno");
        cadastrarUsuario(email, "ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> pedido = esqueciSenha(email);
        String token = tokenDoEmailEnviado(email);
        ResponseEntity<Map<String, Object>> redefinicao = redefinir(token, NOVA_SENHA);

        // Assert: a nova senha entra e a antiga não
        assertThat(pedido.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(redefinicao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logar(email, NOVA_SENHA)).isEqualTo(HttpStatus.OK);
        assertThat(logar(email, SENHA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void esqueciSenha_comEmailDesconhecido_deveResponderIgualSemEnviarNada() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = esqueciSenha(emailUnico("ninguem"));

        // Assert: mesma resposta de um e-mail cadastrado, para não revelar quem tem conta
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(emailService, never()).enviarLinkRedefinicaoSenha(anyString(), anyString());
    }

    @Test
    void esqueciSenha_comEmailInvalido_deveRetornar400() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = esqueciSenha("nao-e-email");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    @Test
    void esqueciSenha_duasVezes_soOLinkMaisNovoDeveFuncionar() {
        // Arrange
        String email = emailUnico("aluno");
        cadastrarUsuario(email, "ALUNO");
        esqueciSenha(email);
        String primeiro = tokenDoEmailEnviado(email);
        esqueciSenha(email);
        String segundo = ultimoTokenEnviado(email);

        // Act + Assert
        assertThat(redefinir(primeiro, NOVA_SENHA).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(redefinir(segundo, NOVA_SENHA).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // ----------------------------------------------- POST /usuarios/senha/redefinir

    @Test
    void redefinir_comTokenInexistente_deveRetornar400ComCodigo() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = redefinir("token-que-nao-existe", NOVA_SENHA);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "LINK_INVALIDO");
    }

    @Test
    void redefinir_comLinkJaUsado_deveRetornar400() {
        // Arrange
        String email = emailUnico("aluno");
        cadastrarUsuario(email, "ALUNO");
        esqueciSenha(email);
        String token = tokenDoEmailEnviado(email);
        redefinir(token, NOVA_SENHA);

        // Act
        ResponseEntity<Map<String, Object>> resposta = redefinir(token, "Outra-Senha-123");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "LINK_INVALIDO");
    }

    @Test
    void redefinir_comLinkExpirado_deveRetornar400() {
        // Arrange: o link é gerado e depois "envelhece" 31 minutos no banco
        String email = emailUnico("aluno");
        Long usuarioId = cadastrarUsuario(email, "ALUNO");
        esqueciSenha(email);
        String token = tokenDoEmailEnviado(email);

        TokenRedefinicaoSenha registro = tokenRepository.findAllByUsuario_IdAndUtilizadoFalse(usuarioId).get(0);
        registro.setExpiracao(LocalDateTime.now().minusMinutes(1));
        tokenRepository.save(registro);

        // Act
        ResponseEntity<Map<String, Object>> resposta = redefinir(token, NOVA_SENHA);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "LINK_INVALIDO");
        assertThat(logar(email, SENHA)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void redefinir_comSenhaCurta_deveRetornar400ComCampoInvalido() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = redefinir("qualquer-token", "123");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    @Test
    void banco_naoDeveGuardarOTokenDoLink() {
        // Arrange
        String email = emailUnico("aluno");
        Long usuarioId = cadastrarUsuario(email, "ALUNO");
        esqueciSenha(email);
        String token = tokenDoEmailEnviado(email);

        // Assert: só o hash fica salvo
        TokenRedefinicaoSenha registro = tokenRepository.findAllByUsuario_IdAndUtilizadoFalse(usuarioId).get(0);
        assertThat(registro.getTokenHash()).isNotEqualTo(token).hasSize(64);
    }

    // ---------------------------------------------------- Bloqueio após 3 erros

    @Test
    void login_comTresSenhasErradas_deveBloquearSemEnviarEmail() {
        // Arrange
        String email = emailUnico("aluno");
        cadastrarUsuario(email, "ALUNO");

        // Act
        HttpStatus primeira = logar(email, SENHA_ERRADA);
        HttpStatus segunda = logar(email, SENHA_ERRADA);
        ResponseEntity<Map<String, Object>> terceira = logarComCorpo(email, SENHA_ERRADA);

        // Assert: as duas primeiras são erro comum; a terceira bloqueia
        assertThat(primeira).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(segunda).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(terceira.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(terceira.getBody()).containsEntry("codigo", "CONTA_BLOQUEADA");

        // Assert: o e-mail só sai quando a pessoa pede pelo "Esqueci minha senha"
        verify(emailService, never()).enviarLinkRedefinicaoSenha(anyString(), anyString());

        // Assert: nem a senha certa entra enquanto estiver bloqueada
        ResponseEntity<Map<String, Object>> comSenhaCerta = logarComCorpo(email, SENHA);
        assertThat(comSenhaCerta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(comSenhaCerta.getBody()).containsEntry("codigo", "CONTA_BLOQUEADA");
    }

    @Test
    void login_bloqueado_deveVoltarAEntrarDepoisDoEsqueciMinhaSenha() {
        // Arrange: bloqueia e depois faz o "Esqueci minha senha"
        String email = emailUnico("aluno");
        Long usuarioId = cadastrarUsuario(email, "ALUNO");
        logar(email, SENHA_ERRADA);
        logar(email, SENHA_ERRADA);
        logar(email, SENHA_ERRADA);
        esqueciSenha(email);
        String token = tokenDoEmailEnviado(email);

        // Act
        ResponseEntity<Map<String, Object>> redefinicao = redefinir(token, NOVA_SENHA);

        // Assert
        assertThat(redefinicao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logar(email, NOVA_SENHA)).isEqualTo(HttpStatus.OK);
        assertThat(usuarioRepository.findById(usuarioId).orElseThrow().getTentativasLogin()).isZero();
    }

    @Test
    void login_comSenhaCertaAntesDaTerceira_deveZerarAsTentativas() {
        // Arrange: dois erros, um acerto, mais dois erros
        String email = emailUnico("aluno");
        cadastrarUsuario(email, "ALUNO");
        logar(email, SENHA_ERRADA);
        logar(email, SENHA_ERRADA);
        logar(email, SENHA);
        logar(email, SENHA_ERRADA);

        // Act
        HttpStatus quinta = logar(email, SENHA_ERRADA);

        // Assert: só 2 erros seguidos, então ainda não bloqueou
        assertThat(quinta).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(logar(email, SENHA)).isEqualTo(HttpStatus.OK);
    }

    @Test
    void login_comEmailDesconhecido_deveContinuarRetornando401() {
        // Act
        String email = emailUnico("ninguem");
        logar(email, SENHA_ERRADA);
        logar(email, SENHA_ERRADA);

        // Assert: e-mail que não existe não bloqueia nem revela nada
        assertThat(logar(email, SENHA_ERRADA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ------------------------------------------------------------------ Helpers

    private ResponseEntity<Map<String, Object>> esqueciSenha(String email) {
        return client.post().uri("/usuarios/senha/esqueci")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> redefinir(String token, String novaSenha) {
        return client.post().uri("/usuarios/senha/redefinir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token, "novaSenha", novaSenha))
                .retrieve()
                .toEntity(JSON);
    }

    /** Confere que o e-mail foi enviado e devolve o token do link. */
    private String tokenDoEmailEnviado(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarLinkRedefinicaoSenha(eq(email), link.capture());
        return extrairToken(link.getValue());
    }

    /** Quando o mesmo e-mail recebeu mais de um link, devolve o token do último. */
    private String ultimoTokenEnviado(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService, org.mockito.Mockito.atLeastOnce())
                .enviarLinkRedefinicaoSenha(eq(email), link.capture());
        return extrairToken(link.getValue());
    }

    private static String extrairToken(String link) {
        assertThat(link).contains("/redefinir-senha?token=");
        return link.substring(link.indexOf("token=") + "token=".length());
    }

    private ResponseEntity<Map<String, Object>> logarComCorpo(String email, String senha) {
        return client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", senha))
                .retrieve()
                .toEntity(JSON);
    }

    /** Primeiro passo do login (usuário e senha); devolve só o status. */
    private HttpStatus logar(String email, String senha) {
        return HttpStatus.valueOf(logarComCorpo(email, senha).getStatusCode().value());
    }
}

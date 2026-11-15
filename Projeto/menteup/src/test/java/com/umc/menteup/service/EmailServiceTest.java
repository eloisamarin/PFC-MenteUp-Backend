package com.umc.menteup.service;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.junit.jupiter.api.Assertions.assertThrows;

/// Envio de e-mail pela API do Brevo. O servidor do Brevo é simulado (nada é enviado de verdade)
class EmailServiceTest {

    private static final String REMETENTE = "projeto@menteup.com";

    @Test
    void deveEnviarPelaApiDoBrevo() {
        // Arrange
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        EmailService service = new EmailService(builder, "chave-de-teste", REMETENTE);

        brevo.expect(requestTo(EmailService.URL_BREVO))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "chave-de-teste"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.sender.email").value(REMETENTE))
                .andExpect(jsonPath("$.sender.name").value("MenteUp"))
                .andExpect(jsonPath("$.to[0].email").value("aluno@teste.com"))
                .andExpect(jsonPath("$.subject").value("MenteUp - Código de verificação"))
                .andExpect(jsonPath("$.textContent").value(org.hamcrest.Matchers.containsString("123456")))
                .andRespond(withStatus(HttpStatus.CREATED));

        // Act
        service.enviarCodigo2FA("aluno@teste.com", "123456");

        // Assert
        brevo.verify();
    }

    @Test
    void semChaveDoBrevo_naoDeveSubir() {
        assertThrows(IllegalStateException.class,
                () -> new EmailService(RestClient.builder(), "", REMETENTE));
    }

    @Test
    void semRemetente_naoDeveSubir() {
        assertThrows(IllegalStateException.class,
                () -> new EmailService(RestClient.builder(), "chave-de-teste", " "));
    }

    @Test
    void erroNoBrevo_deveSubirParaQuemChamouTratar() {
        // No login, o código não enviado conta como falha (UsuarioService trata a exceção)
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer brevo = MockRestServiceServer.bindTo(builder).build();
        EmailService service = new EmailService(builder, "chave-de-teste", REMETENTE);
        brevo.expect(requestTo(EmailService.URL_BREVO)).andRespond(withServerError());

        assertThrows(RestClientException.class,
                () -> service.enviarConvitePrimeiroAcesso("aluno@teste.com", "Aluno", "https://menteup/link"));
    }
}

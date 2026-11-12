package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.Codigo2FA;

/*
 * Código de 2 etapas (2FA):
 * - contra força bruta, cada código aceita no máximo 5 erros e depois deixa de valer;
 * - "Reenviar código": só depois de conferir a senha no /logar, com 60 s de espera
 *   entre os envios e no máximo 3 reenvios; o código anterior deixa de valer.
 */
class DoisFatoresIntegrationTest extends IntegracaoBase {

    private static final String CODIGO_ERRADO_BASE = "00000";

    @Test
    void codigoErrado_deveInformarQuantasTentativasRestam() {
        String email = logarComSenha();

        ResponseEntity<Map<String, Object>> resposta = validar(email, codigoErrado(email, 0));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resposta.getBody()).containsEntry("codigo", "CODIGO_2FA_INCORRETO");
        assertThat((String) resposta.getBody().get("mensagem")).contains("Restam 4 tentativas");

        // Ainda dentro do limite: o código certo funciona
        assertThat(validar(email, codigo2FAPendente(email)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void cincoErros_deveInutilizarOCodigoMesmoQueDepoisAcerte() {
        // Arrange
        String email = logarComSenha();
        String certo = codigo2FAPendente(email);

        // Act: 4 erros e o 5º bloqueia
        for (int i = 0; i < 4; i++) {
            assertThat(validar(email, codigoErrado(email, i)).getBody())
                    .containsEntry("codigo", "CODIGO_2FA_INCORRETO");
        }
        ResponseEntity<Map<String, Object>> quinto = validar(email, codigoErrado(email, 4));
        ResponseEntity<Map<String, Object>> depois = validar(email, certo);

        // Assert
        assertThat(quinto.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(quinto.getBody()).containsEntry("codigo", "CODIGO_2FA_BLOQUEADO");
        assertThat(depois.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(depois.getBody()).containsEntry("codigo", "CODIGO_2FA_INVALIDO");
    }

    @Test
    void codigoBloqueado_podeSerSubstituidoPeloReenvio() {
        String email = logarComSenha();
        for (int i = 0; i < 5; i++) {
            validar(email, codigoErrado(email, i));
        }
        envelhecerUltimoCodigo(email);

        assertThat(reenviar(email).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(validar(email, codigo2FAPendente(email)).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reenviar_logoDepoisDoEnvio_deveMandarAguardar() {
        String email = logarComSenha();

        ResponseEntity<Map<String, Object>> resposta = reenviar(email);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(resposta.getBody()).containsEntry("codigo", "AGUARDE_REENVIO_2FA");
    }

    @Test
    void reenviar_depoisDaEspera_deveMandarNovoCodigoEInvalidarOAnterior() {
        // Arrange
        String email = logarComSenha();
        String antigo = codigo2FAPendente(email);
        envelhecerUltimoCodigo(email);

        // Act
        ResponseEntity<Map<String, Object>> resposta = reenviar(email);

        // Assert: e-mail enviado de novo; o código antigo não vale mais e o novo vale
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(emailService, times(2)).enviarCodigo2FA(eq(email), anyString());
        String novo = codigo2FAPendente(email);
        if (!novo.equals(antigo)) {
            assertThat(validar(email, antigo).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
        assertThat(validar(email, novo).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void reenviar_maisDeTresVezes_deveExigirNovoLogin() {
        String email = logarComSenha();
        for (int i = 0; i < 3; i++) {
            envelhecerUltimoCodigo(email);
            assertThat(reenviar(email).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
        envelhecerUltimoCodigo(email);

        ResponseEntity<Map<String, Object>> quarto = reenviar(email);

        assertThat(quarto.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(quarto.getBody()).containsEntry("codigo", "LIMITE_REENVIO_2FA");
    }

    @Test
    void reenviar_semTerEntradoComASenha_deveRecusar() {
        // Sem /logar recente não há código: ninguém dispara e-mails para qualquer endereço
        ResponseEntity<Map<String, Object>> resposta = reenviar(emailUnico("desconhecido"));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_LOGIN_RECENTE");
    }

    @Test
    void reenviar_muitoTempoDepoisDoLogin_deveExigirNovoLogin() {
        String email = logarComSenha();
        Codigo2FA codigo = codigo2FARepository.findTopByUsuarioOrderByIdDesc(email).orElseThrow();
        codigo.setExpiracao(codigo.getExpiracao().minusMinutes(30));
        codigo2FARepository.save(codigo);

        ResponseEntity<Map<String, Object>> resposta = reenviar(email);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_LOGIN_RECENTE");
    }

    // ------------------------------------------------------------- Helpers

    /** Cadastra um aluno e faz só o passo da senha (o código fica pendente). */
    private String logarComSenha() {
        String email = emailUnico("dois.fatores");
        cadastrarUsuario(email, "ALUNO");

        ResponseEntity<Void> login = client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();
        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);
        return email;
    }

    /** Um código de 6 dígitos garantidamente diferente do certo. */
    private String codigoErrado(String email, int variacao) {
        String certo = codigo2FAPendente(email);
        String errado = CODIGO_ERRADO_BASE + variacao;
        return errado.equals(certo) ? "99999" + variacao : errado;
    }

    /** Faz o último código parecer ter sido enviado há 2 minutos (passa a espera do reenvio). */
    private void envelhecerUltimoCodigo(String email) {
        Codigo2FA codigo = codigo2FARepository.findTopByUsuarioOrderByIdDesc(email).orElseThrow();
        codigo.setExpiracao(codigo.getExpiracao().minusMinutes(2));
        codigo2FARepository.save(codigo);
    }

    private ResponseEntity<Map<String, Object>> validar(String email, String codigo) {
        return client.post().uri("/usuarios/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigo))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> reenviar(String email) {
        return client.post().uri("/usuarios/reenviar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email))
                .retrieve()
                .toEntity(JSON);
    }
}

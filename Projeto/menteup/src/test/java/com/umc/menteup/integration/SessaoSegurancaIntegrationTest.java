package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.Codigo2FA;

// Logins abertos caem quando a senha muda ou a conta é bloqueada, e o código 2FA não fica legível no banco
class SessaoSegurancaIntegrationTest extends IntegracaoBase {

    private static final String NOVA_SENHA = "Bb2@" + System.nanoTime();

    @Test
    void trocarASenha_derrubaOLoginQueJaEstavaAberto() {
        Sessao aluno = novaSessao("ALUNO");
        assertThat(meusDados(aluno.token()).getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Void> troca = client.put().uri("/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", SENHA, "novaSenha", NOVA_SENHA))
                .retrieve()
                .toBodilessEntity();
        assertThat(troca.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        ResponseEntity<Map<String, Object>> depois = meusDados(aluno.token());
        assertThat(depois.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(depois.getBody()).containsEntry("codigo", "TOKEN_INVALIDO");
    }

    @Test
    void contaBloqueada_derrubaOLoginQueJaEstavaAberto() {
        Sessao aluno = novaSessao("ALUNO");

        for (int i = 0; i < 3; i++) {
            logar(aluno.email(), "senha-errada-" + i);
        }

        assertThat(meusDados(aluno.token()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void redefinirPeloLink_derrubaOLoginAntigoEONovoFunciona() {
        Sessao aluno = novaSessao("ALUNO");

        esqueciSenha(aluno.email());
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarLinkRedefinicaoSenha(eq(aluno.email()), link.capture());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());

        ResponseEntity<Void> redefinicao = client.post().uri("/usuarios/senha/redefinir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token, "novaSenha", NOVA_SENHA))
                .retrieve()
                .toBodilessEntity();
        assertThat(redefinicao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        assertThat(meusDados(aluno.token()).getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void codigo2FA_naoFicaEmTextoNoBanco() {
        String email = emailUnico("hash.2fa");
        cadastrarUsuario(email, "ALUNO");
        assertThat(logar(email, SENHA)).isEqualTo(HttpStatus.OK);

        String enviado = codigo2FAPendente(email);
        Codigo2FA gravado = codigo2FARepository.findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(email).orElseThrow();

        assertThat(gravado.getCodigo()).isNotEqualTo(enviado).doesNotContain(enviado);
        assertThat(autenticar(email)).startsWith("Bearer ");
    }

    @Test
    void esqueciSenha_muitosPedidosDoMesmoEmail_soEnviaAteOLimite() {
        Sessao aluno = novaSessao("ALUNO");

        for (int i = 0; i < 5; i++) {
            assertThat(esqueciSenha(aluno.email()).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }

        // A resposta é sempre a mesma, mas só os 3 primeiros pedidos da hora viram e-mail
        verify(emailService, times(3)).enviarLinkRedefinicaoSenha(eq(aluno.email()), anyString());
    }

    private ResponseEntity<Map<String, Object>> meusDados(String token) {
        return client.get().uri("/usuarios/me")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Void> esqueciSenha(String email) {
        return client.post().uri("/usuarios/senha/esqueci")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email))
                .retrieve()
                .toBodilessEntity();
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

package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;

/*
 * Usuário criado pelo administrador: recebe um convite por e-mail (link de 3 dias)
 * para criar a própria senha e aceitar o Termo de Aceite e a Política de Privacidade.
 * O EmailService é mockado: o link é capturado do mock.
 */
class PrimeiroAcessoIntegrationTest extends IntegracaoBase {

    private static final String NOVA_SENHA = "senha-do-convite-123";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void criarPeloAdministrador_deveEnviarConviteEDeixarAContaSemAcesso() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(administrador, email, "PROFESSOR");

        // Assert: criado, aguardando o primeiro acesso, convite enviado e sem como entrar ainda
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("aguardandoPrimeiroAcesso", true);
        assertThat(tokenDoConvite(email)).isNotBlank();
        assertThat(logar(email, NOVA_SENHA)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void primeiroAcesso_comAceite_deveCriarASenhaRegistrarOTermoELiberarOLogin() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        Long id = id(criar(administrador, email, "ALUNO").getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta = concluir(tokenDoConvite(email), true);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logar(email, NOVA_SENHA)).isEqualTo(HttpStatus.OK);

        Usuario usuario = usuarioRepository.findById(id).orElseThrow();
        assertThat(usuario.getTermoAceitoEm()).isNotNull();
        assertThat(usuario.isPrimeiroAcessoPendente()).isFalse();
    }

    @Test
    void primeiroAcesso_semAceitarOTermo_deveRetornar400ENaoCriarASenha() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        criar(administrador, email, "ALUNO");
        String token = tokenDoConvite(email);

        // Act
        ResponseEntity<Map<String, Object>> resposta = concluir(token, false);

        // Assert: recusado; e o mesmo link continua valendo para tentar de novo, agora aceitando
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "TERMO_NAO_ACEITO");
        assertThat(logar(email, NOVA_SENHA)).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(concluir(token, true).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void conviteUsado_naoDeveFuncionarDeNovo() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        criar(administrador, email, "ALUNO");
        String token = tokenDoConvite(email);
        concluir(token, true);

        ResponseEntity<Map<String, Object>> resposta = concluir(token, true);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "LINK_INVALIDO");
    }

    @Test
    void linkDoConvite_naoDeveServirParaRedefinirSenhaSemOTermo() {
        // O /redefinir não pede o aceite: o convite não pode ser usado ali
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        criar(administrador, email, "ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/senha/redefinir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", tokenDoConvite(email), "novaSenha", NOVA_SENHA))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "LINK_INVALIDO");
    }

    @Test
    void esqueciSenha_deQuemAindaNaoFezPrimeiroAcesso_deveReenviarOConvite() {
        // Arrange: o convite "expirou" e a pessoa usa o "Esqueci minha senha"
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        criar(administrador, email, "ALUNO");

        // Act
        client.post().uri("/usuarios/senha/esqueci")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", email))
                .retrieve()
                .toBodilessEntity();

        // Assert: recebe outro convite (com o termo), e não o link comum de redefinição
        ArgumentCaptor<String> links = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarConvitePrimeiroAcesso(eq(email), anyString(), links.capture());
        assertThat(links.getAllValues()).hasSize(2);
        verify(emailService, never()).enviarLinkRedefinicaoSenha(eq(email), anyString());

        // O convite antigo deixa de valer; o novo funciona
        String antigo = extrairToken(links.getAllValues().get(0));
        String novo = extrairToken(links.getAllValues().get(1));
        assertThat(concluir(antigo, true).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(concluir(novo, true).getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void listaDeUsuarios_deveMostrarQuemAindaNaoFezPrimeiroAcesso() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("convidado");
        Long id = id(criar(administrador, email, "ALUNO").getBody());

        List<Map<String, Object>> usuarios = client.get().uri("/usuarios/all")
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();

        assertThat(usuarios)
                .filteredOn(u -> id.equals(id(u)))
                .singleElement()
                .satisfies(u -> assertThat(u).containsEntry("aguardandoPrimeiroAcesso", true)
                        .doesNotContainKey("termoAceitoEm")
                        .doesNotContainKey("primeiroAcessoPendente"));
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> criar(Sessao administrador, String email, String tipo) {
        return client.post().uri("/usuarios")
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nomeUsuario", "Convidado Teste", "usuario", email, "tipoUsuario", tipo))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> concluir(String token, boolean aceitouTermos) {
        return client.post().uri("/usuarios/senha/primeiro-acesso")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token, "novaSenha", NOVA_SENHA, "aceitouTermos", aceitouTermos))
                .retrieve()
                .toEntity(JSON);
    }

    /** Pega o token do último convite enviado para este e-mail. */
    private String tokenDoConvite(String email) {
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService, atLeastOnce()).enviarConvitePrimeiroAcesso(eq(email), anyString(), link.capture());
        assertThat(link.getValue()).contains("/primeiro-acesso?token=");
        return extrairToken(link.getValue());
    }

    private static String extrairToken(String link) {
        return link.substring(link.indexOf("token=") + "token=".length());
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

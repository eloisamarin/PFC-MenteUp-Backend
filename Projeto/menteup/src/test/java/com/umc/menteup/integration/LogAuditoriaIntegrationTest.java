package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.LogSistema;
import com.umc.menteup.model.ResultadoLog;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.UsuarioRepository;

// Logs de auditoria: sem nome da pessoa no texto e com as ações de conta registradas
class LogAuditoriaIntegrationTest extends IntegracaoBase {

    // Atende às regras de senha forte e é diferente da senha dos testes
    private static final String NOVA_SENHA = "Bb2@" + UUID.randomUUID();

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void login_naoDeveGravarONomeNoTextoDoLog() {
        Sessao aluno = novaSessao("ALUNO");
        String nome = usuarioRepository.findById(aluno.id()).orElseThrow().getNomeUsuario();

        List<LogSistema> logins = logsDo(aluno.id(), AcaoLog.LOGIN);

        assertThat(logins).isNotEmpty()
                .allSatisfy(l -> assertThat(l.getDescricao()).contains("#" + aluno.id()).doesNotContain(nome));
    }

    @Test
    void excluirConta_deveTirarONomeDosLogsAntigosERegistrarOPedido() {
        // Arrange: um log antigo, do tempo em que o nome ia no texto
        Sessao aluno = novaSessao("ALUNO");
        String nome = nomeUnico("Fulana");
        Usuario usuario = usuarioRepository.findById(aluno.id()).orElseThrow();
        usuario.setNomeUsuario(nome);
        usuarioRepository.save(usuario);
        Long logAntigo = gravarLogAntigo(usuario, "Aluno \"" + nome + "\" entrou no sistema.");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/me/exclusao")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senha", SENHA))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logSistemaRepository.findById(logAntigo).orElseThrow().getDescricao())
                .doesNotContain(nome)
                .contains("\"Usuário removido\"");
        assertThat(logsDo(aluno.id(), AcaoLog.EXCLUIR_PROPRIA_CONTA)).singleElement()
                .satisfies(l -> assertThat(l.getDescricao()).doesNotContain(nome));
    }

    @Test
    void alterarSenha_deveFicarRegistrada() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/me/senha")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("senhaAtual", SENHA, "novaSenha", NOVA_SENHA))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logsDo(aluno.id(), AcaoLog.ALTERAR_SENHA)).hasSize(1);
    }

    @Test
    void redefinirSenhaPeloLink_deveFicarRegistrada() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        client.post().uri("/usuarios/senha/esqueci")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("email", aluno.email()))
                .retrieve()
                .toBodilessEntity();
        ArgumentCaptor<String> link = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarLinkRedefinicaoSenha(eq(aluno.email()), link.capture());
        String token = link.getValue().substring(link.getValue().indexOf("token=") + "token=".length());

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/senha/redefinir")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", token, "novaSenha", NOVA_SENHA))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(logsDo(aluno.id(), AcaoLog.REDEFINIR_SENHA)).hasSize(1);
    }

    @Test
    void consultarLogs_comoAdministrador_deveFicarRegistrado() {
        Sessao administrador = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = client.get().uri("/logs?acao=LOGIN")
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(logsDo(administrador.id(), AcaoLog.CONSULTAR_LOGS)).singleElement()
                .satisfies(l -> assertThat(l.getDados()).contains("LOGIN"));
    }

    private List<LogSistema> logsDo(Long usuarioId, AcaoLog acao) {
        return logSistemaRepository.findAll().stream()
                .filter(l -> l.getAcao() == acao)
                .filter(l -> l.getUsuario() != null && usuarioId.equals(l.getUsuario().getId()))
                .toList();
    }

    private Long gravarLogAntigo(Usuario usuario, String descricao) {
        LogSistema log = new LogSistema();
        log.setUsuario(usuario);
        log.setAcao(AcaoLog.LOGIN);
        log.setResultado(ResultadoLog.SUCESSO);
        log.setDescricao(descricao);
        return logSistemaRepository.save(log).getId();
    }
}

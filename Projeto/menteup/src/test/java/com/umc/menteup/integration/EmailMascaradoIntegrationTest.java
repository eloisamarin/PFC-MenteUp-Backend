package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import com.umc.menteup.security.MascaraDados;

// O e-mail de outra pessoa aparece mascarado; o administrador revela um por vez, com registro no log
class EmailMascaradoIntegrationTest extends IntegracaoBase {

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void administradorRevelaOEmail_eAConsultaFicaNoLog() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = revelar(admin, aluno.id());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("email", aluno.email());
        assertThat(resposta.getHeaders().getCacheControl()).contains("no-store");
        assertThat(logSistemaRepository.findAll())
                .anyMatch(l -> l.getAcao() == AcaoLog.VISUALIZAR_EMAIL
                        && aluno.id().equals(l.getEntidadeId())
                        && l.getUsuario() != null && admin.id().equals(l.getUsuario().getId()));
    }

    @Test
    void professorEAluno_naoRevelamEmail() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");

        assertThat(revelarSemCorpo(professor, aluno.id()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(revelarSemCorpo(aluno, professor.id()).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void revelarAlemDoLimitePorHora_eRecusado() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        // Nos testes o limite é 3 por hora
        for (int i = 0; i < 3; i++) {
            assertThat(revelar(admin, aluno.id()).getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        ResponseEntity<Map<String, Object>> excesso = revelar(admin, aluno.id());
        assertThat(excesso.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(excesso.getBody()).containsEntry("codigo", "MUITOS_PEDIDOS");
    }

    @Test
    void usuarioQueNaoExiste_devolve404() {
        Sessao admin = novaSessao("ADMINISTRADOR");

        assertThat(revelar(admin, Long.MAX_VALUE).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void administradorCorrigeONomeSemMandarOEmail_eORespostaVemMascarada() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        Map<String, Object> dados = new HashMap<>();
        dados.put("id", aluno.id());
        dados.put("nomeUsuario", "Nome Corrigido");
        dados.put("tipoUsuario", "ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("nomeUsuario", "Nome Corrigido")
                .containsEntry("emailMascarado", MascaraDados.email(aluno.email()))
                .doesNotContainKey("usuario");
        // O e-mail continua o mesmo
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getUsuario()).isEqualTo(aluno.email());
    }

    private ResponseEntity<Map<String, Object>> revelar(Sessao sessao, Long id) {
        return client.get().uri("/usuarios/{id}/email", id)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Void> revelarSemCorpo(Sessao sessao, Long id) {
        return client.get().uri("/usuarios/{id}/email", id)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();
    }
}

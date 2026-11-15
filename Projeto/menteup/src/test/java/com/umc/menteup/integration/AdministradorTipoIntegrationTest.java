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

import com.umc.menteup.repository.UsuarioRepository;

// Conta de administrador mantém o tipo: nem outro administrador nem ela mesma podem rebaixá-la
class AdministradorTipoIntegrationTest extends IntegracaoBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void administrador_naoRebaixaOutroAdministrador() {
        Sessao autor = novaSessao("ADMINISTRADOR");
        Sessao outro = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = atualizar(autor, outro, "Outro Admin", "PROFESSOR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "REBAIXAR_ADMINISTRADOR_NEGADO");
        assertThat(tipoNoBanco(outro)).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void administrador_naoRebaixaASiMesmo() {
        Sessao autor = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = atualizar(autor, autor, "Eu Mesmo", "ALUNO");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "REBAIXAR_ADMINISTRADOR_NEGADO");
        assertThat(tipoNoBanco(autor)).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void administrador_continuaEditandoNomeDeOutroAdministrador() {
        Sessao autor = novaSessao("ADMINISTRADOR");
        Sessao outro = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = atualizar(autor, outro, "Nome Corrigido", "ADMINISTRADOR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(usuarioRepository.findById(outro.id()).orElseThrow().getNomeUsuario())
                .isEqualTo("Nome Corrigido");
    }

    @Test
    void administrador_continuaMudandoOTipoDeAlunoEProfessor() {
        Sessao autor = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = atualizar(autor, aluno, "Aluno Promovido", "PROFESSOR");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(tipoNoBanco(aluno)).isEqualTo("PROFESSOR");
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao autor, Sessao alvo, String nome, String tipo) {
        Map<String, Object> dados = new HashMap<>(buildUsuario(nome, alvo.email(), tipo));
        dados.put("id", alvo.id());
        return client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);
    }

    private String tipoNoBanco(Sessao sessao) {
        return usuarioRepository.findById(sessao.id()).orElseThrow().getTipoUsuario().toUpperCase();
    }
}

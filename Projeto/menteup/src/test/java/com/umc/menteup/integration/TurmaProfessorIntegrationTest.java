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

import com.umc.menteup.repository.TurmaRepository;

/*
 * Professor responsável pela turma:
 * - o professor que cria a turma fica responsável por ela;
 * - o administrador escolhe (opcional) o professor ao criar ou editar a turma;
 * - o professor responsável passa a ter acesso às atividades e desafios da turma.
 */
class TurmaProfessorIntegrationTest extends IntegracaoBase {

    @Autowired
    private TurmaRepository turmaRepository;

    @Test
    void professorQueCriaATurma_ficaResponsavel() {
        Sessao professor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> resposta = criar(professor, nomeUnico("Turma"), null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(professorResponsavel(resposta.getBody())).containsEntry("id", professor.id().intValue());
    }

    @Test
    void administrador_comProfessorEscolhido_deveDarAcessoAoProfessor() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(administrador, nomeUnico("Turma"), professor.id());

        // Assert: vinculado, e o professor já consegue criar atividade na turma
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(professorResponsavel(resposta.getBody()))
                .containsEntry("id", professor.id().intValue())
                .containsKey("nome");
        assertThat(resposta.getBody()).doesNotContainKey("professorId");

        Long turmaId = id(resposta.getBody());
        criarAtividade(professor, turmaId, "PENDENTE"); // o helper confere o 201
    }

    @Test
    void administrador_semProfessor_turmaFicaSemResponsavel() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao professor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> resposta = criar(administrador, nomeUnico("Turma"), null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody().get("professorResponsavel")).isNull();
        assertThat(postarAtividade(professor, id(resposta.getBody())).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void administrador_comAlunoOuIdInexistente_deveRecusar() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> comAluno = criar(administrador, nomeUnico("Turma"), aluno.id());
        ResponseEntity<Map<String, Object>> inexistente = criar(administrador, nomeUnico("Turma"), 999_999L);

        assertThat(comAluno.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(comAluno.getBody()).containsEntry("codigo", "USUARIO_NAO_E_PROFESSOR");
        assertThat(inexistente.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(inexistente.getBody()).containsEntry("codigo", "PROFESSOR_NAO_ENCONTRADO");
    }

    @Test
    void professor_naoPodeEscolherOutroProfessor() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        String nome = nomeUnico("Turma");

        ResponseEntity<Map<String, Object>> resposta = criar(professor, nome, outro.id());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_ADMINISTRADOR_VINCULA_PROFESSOR");
        assertThat(turmaRepository.findAllByNomeContainingIgnoreCase(nome)).isEmpty();
    }

    @Test
    void administrador_aoTrocarOProfessor_passaOAcessoParaONovo() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao antigo = novaSessao("PROFESSOR");
        Sessao novo = novaSessao("PROFESSOR");
        String nome = nomeUnico("Turma");
        Long turmaId = id(criar(administrador, nome, antigo.id()).getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(administrador, turmaId, nome, novo.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(professorResponsavel(resposta.getBody())).containsEntry("id", novo.id().intValue());
        assertThat(postarAtividade(novo, turmaId).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(postarAtividade(antigo, turmaId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void administrador_aoRenomearSemInformarProfessor_mantemOResponsavel() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = id(criar(administrador, nomeUnico("Turma"), professor.id()).getBody());

        ResponseEntity<Map<String, Object>> resposta = atualizar(administrador, turmaId, nomeUnico("Renomeada"), null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(professorResponsavel(resposta.getBody())).containsEntry("id", professor.id().intValue());
    }

    @Test
    void professor_naoDeveRenomearNemExcluirTurmaDeOutroProfessor() {
        // Arrange
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(dono);

        // Act
        ResponseEntity<Map<String, Object>> renomear = atualizar(outro, turmaId, nomeUnico("Invadida"), null);
        ResponseEntity<Map<String, Object>> excluir = client.delete().uri("/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, outro.token())
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(renomear.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(renomear.getBody()).containsEntry("codigo", "SEM_ACESSO_A_TURMA");
        assertThat(excluir.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(turmaRepository.existsById(turmaId)).isTrue();
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> criar(Sessao autor, String nome, Long professorId) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("nome", nome);
        if (professorId != null) corpo.put("professorId", professorId);

        return client.post().uri("/turmas")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao autor, Long turmaId, String nome, Long professorId) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("id", turmaId);
        corpo.put("nome", nome);
        if (professorId != null) corpo.put("professorId", professorId);

        return client.put().uri("/turmas")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Void> postarAtividade(Sessao autor, Long turmaId) {
        return client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(nomeUnico("Atividade"), turmaId, "PENDENTE"))
                .retrieve()
                .toBodilessEntity();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> professorResponsavel(Map<String, Object> turma) {
        assertThat(turma.get("professorResponsavel")).isNotNull();
        return (Map<String, Object>) turma.get("professorResponsavel");
    }
}

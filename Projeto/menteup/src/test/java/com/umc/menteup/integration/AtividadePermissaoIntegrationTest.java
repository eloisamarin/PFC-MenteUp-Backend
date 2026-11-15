package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.repository.AtividadeRepository;

/*
 * Criar, editar e excluir atividade: só professor e administrador.
 * O professor só mexe nas atividades das turmas dele; o administrador, em todas.
 * O aluno só vê a atividade e salva o próprio progresso.
 * A listagem completa (com as respostas certas) não é liberada para o aluno
 */
class AtividadePermissaoIntegrationTest extends IntegracaoBase {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Test
    void aluno_naoDeveCriarAtividade() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        String titulo = nomeUnico("Atividade do aluno");

        // Act
        ResponseEntity<Void> resposta = client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(titulo, turmaId, "EM_ANDAMENTO"))
                .retrieve()
                .toBodilessEntity();

        // Assert: recusado e nada foi gravado
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)).isEmpty();
    }

    @Test
    void aluno_naoDeveEditarAtividade() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");

        Map<String, Object> dados = new HashMap<>(buildAtividade("Editada pelo aluno", turmaId, "EM_ANDAMENTO"));
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Void> resposta = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTitulo())
                .isNotEqualTo("Editada pelo aluno");
    }

    @Test
    void aluno_naoDeveExcluirAtividade() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "EM_ANDAMENTO");

        // Act
        ResponseEntity<Void> resposta = client.delete().uri("/atividades/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.existsById(atividadeId)).isTrue();
    }

    @Test
    void professorEAdministrador_devemCriarEditarEExcluir() {
        for (String tipo : new String[] {"PROFESSOR", "ADMINISTRADOR"}) {
            // Arrange
            Sessao autor = novaSessao(tipo);
            Long turmaId = criarTurma(autor);

            // Act + Assert: criar (o helper já confere o 201)
            Long atividadeId = criarAtividade(autor, turmaId, "PENDENTE");

            Map<String, Object> dados = new HashMap<>(buildAtividade("Editada por " + tipo, turmaId, "PENDENTE"));
            dados.put("id", atividadeId);
            ResponseEntity<Void> edicao = client.put().uri("/atividades")
                    .header(HttpHeaders.AUTHORIZATION, autor.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(dados)
                    .retrieve()
                    .toBodilessEntity();
            assertThat(edicao.getStatusCode()).isEqualTo(HttpStatus.OK);

            ResponseEntity<Void> exclusao = client.delete().uri("/atividades/{id}", atividadeId)
                    .header(HttpHeaders.AUTHORIZATION, autor.token())
                    .retrieve()
                    .toBodilessEntity();
            assertThat(exclusao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        }
    }

    @Test
    void alunoDaTurma_deveContinuarVendoAAtividade() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");

        assertThat(buscarAtividade(aluno, atividadeId).getStatusCode()).isEqualTo(HttpStatus.OK);
    }
    // ------------------------------------------------------------- Professor só nas turmas dele

    @Test
    void professor_naoDeveCriarAtividadeNaTurmaDeOutroProfessor() {
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaDoDono = criarTurma(dono);
        String titulo = nomeUnico("Intrusa");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, outro.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(titulo, turmaDoDono, "PENDENTE"))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_ACESSO_A_ATIVIDADE");
        assertThat(atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)).isEmpty();
    }

    @Test
    void professor_naoDeveEditarNemExcluirAtividadeDeOutroProfessor() {
        // Arrange
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaDoDono = criarTurma(dono);
        Long atividadeId = criarAtividade(dono, turmaDoDono, "PENDENTE");

        Map<String, Object> dados = new HashMap<>(buildAtividade("Editada pelo outro", turmaDoDono, "PENDENTE"));
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Void> edicao = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, outro.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();
        ResponseEntity<Void> exclusao = client.delete().uri("/atividades/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, outro.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(edicao.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exclusao.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTitulo())
                .isNotEqualTo("Editada pelo outro");
    }

    @Test
    void professor_naoDeveMoverAtividadeParaATurmaDeOutroProfessor() {
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaDoDono = criarTurma(dono);
        Long turmaDoOutro = criarTurma(outro);
        Long atividadeId = criarAtividade(dono, turmaDoDono, "PENDENTE");

        Map<String, Object> dados = new HashMap<>(buildAtividade("Mudando de turma", turmaDoOutro, "PENDENTE"));
        dados.put("id", atividadeId);

        ResponseEntity<Void> resposta = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, dono.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTurma().getId()).isEqualTo(turmaDoDono);
    }

    @Test
    void professor_naoDeveCadastrarQuestoesNaAtividadeDeOutroProfessor() {
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(dono, criarTurma(dono), "PENDENTE");

        ResponseEntity<Map<String, Object>> resposta = client.post()
                .uri("/atividades/{id}/questionario", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, outro.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntas", List.of(Map.of(
                        "enunciado", "Pergunta intrusa",
                        "alternativas", List.of(
                                Map.of("texto", "A", "correta", true),
                                Map.of("texto", "B", "correta", false))))))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SEM_ACESSO_A_ATIVIDADE");
    }

    @Test
    void administrador_deveEditarEExcluirAtividadeDeQualquerProfessor() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");
        cadastrarQuestionario(administrador, atividadeId, 1);

        Map<String, Object> dados = new HashMap<>(buildAtividade("Editada pelo admin", turmaId, "PENDENTE"));
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Void> edicao = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();
        ResponseEntity<Void> exclusao = client.delete().uri("/atividades/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(edicao.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(exclusao.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(atividadeRepository.existsById(atividadeId)).isFalse();
    }

    // A listagem completa traz as respostas certas: o aluno usa /alunos/me/atividades
    @Test
    void aluno_naoDeveListarAtividadesComRespostas() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        criarAtividade(professor, turmaId, "EM_ANDAMENTO");

        // Act
        ResponseEntity<Void> lista = client.get().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toBodilessEntity();
        ResponseEntity<Void> porTitulo = client.get().uri("/atividades/titulo/{titulo}", "Atividade")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(porTitulo.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void professor_deveListarSoAsAtividadesDasTurmasDele() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long minha = criarAtividade(professor, criarTurma(professor), "PENDENTE");
        Long deOutro = criarAtividade(outro, criarTurma(outro), "PENDENTE");

        // Act
        List<Long> ids = idsDe(professor, "/atividades");

        // Assert
        assertThat(ids).contains(minha).doesNotContain(deOutro);
    }

    @Test
    void professor_buscaPorTitulo_naoDeveTrazerAtividadeDeOutroProfessor() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        String titulo = nomeUnico("Titulo compartilhado");
        Long deOutro = idDaCriada(outro, criarTurma(outro), titulo);

        // Act
        List<Long> doProfessor = idsDe(professor, "/atividades/titulo/{titulo}", titulo);
        List<Long> doAdministrador = idsDe(novaSessao("ADMINISTRADOR"), "/atividades/titulo/{titulo}", titulo);

        // Assert
        assertThat(doProfessor).doesNotContain(deOutro);
        assertThat(doAdministrador).contains(deOutro);
    }

    private List<Long> idsDe(Sessao sessao, String uri, Object... variaveis) {
        return client.get().uri(uri, variaveis)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody()
                .stream()
                .map(IntegracaoBase::id)
                .toList();
    }

    private Long idDaCriada(Sessao autor, Long turmaId, String titulo) {
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(titulo, turmaId, "PENDENTE"))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return id(resposta.getBody());
    }
}

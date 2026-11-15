package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.repository.AtividadeRepository;

///Uma atividade atribuída a mais de uma turma do mesmo professor
class AtividadeVariasTurmasIntegrationTest extends IntegracaoBase {

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Test
    @SuppressWarnings("unchecked")
    void criar_comDuasTurmasDoProfessor_deveGravarAsDuas() {
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);

        ResponseEntity<Map<String, Object>> resposta =
                criar(professor, atividadeComTurmas("PENDENTE", turmaA, turmaB));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map<String, Object>> turmas = (List<Map<String, Object>>) resposta.getBody().get("turmas");
        assertThat(turmas).extracting(IntegracaoBase::id).containsExactlyInAnyOrder(turmaA, turmaB);
        assertThat(atividadeRepository.findById(id(resposta.getBody())).orElseThrow().getTurmas())
                .hasSize(2);
    }

    @Test
    void alunoDaSegundaTurma_deveVerEAbrirAAtividade() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);
        vincularAluno(professor, turmaB, aluno.id());
        Long atividadeId = id(criar(professor, atividadeComTurmas("EM_ANDAMENTO", turmaA, turmaB)).getBody());
        cadastrarQuestionario(professor, atividadeId, 1);

        assertThat(buscarAtividade(aluno, atividadeId).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(idsDasAtividadesDoAluno(aluno)).containsExactly(atividadeId);
    }

    @Test
    void alunoNasDuasTurmas_deveReceberAAtividadeUmaVezSo() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);
        vincularAluno(professor, turmaA, aluno.id());
        vincularAluno(professor, turmaB, aluno.id());
        Long atividadeId = id(criar(professor, atividadeComTurmas("EM_ANDAMENTO", turmaA, turmaB)).getBody());

        assertThat(idsDasAtividadesDoAluno(aluno)).containsExactly(atividadeId);
    }

    @Test
    void criar_comTurmaDeOutroProfessor_deveRetornar403ENaoGravar() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Long turmaPropria = criarTurma(professor);
        Long turmaDoOutro = criarTurma(outroProfessor);
        long antes = atividadeRepository.count();

        ResponseEntity<Map<String, Object>> resposta =
                criar(professor, atividadeComTurmas("PENDENTE", turmaPropria, turmaDoOutro));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(atividadeRepository.count()).isEqualTo(antes);
    }

    @Test
    void editar_tirandoUmaTurma_deveFicarSoComARestante() {
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);
        Long atividadeId = id(criar(professor, atividadeComTurmas("PENDENTE", turmaA, turmaB)).getBody());

        Map<String, Object> dados = atividadeComTurmas("PENDENTE", turmaB);
        dados.put("id", atividadeId);
        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTurmas())
                .extracting(t -> t.getId())
                .containsExactly(turmaB);
    }

    @Test
    void excluirTurma_deveApagarAAtividadeCompartilhada() {
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaA = criarTurma(professor);
        Long turmaB = criarTurma(professor);
        Long atividadeId = id(criar(professor, atividadeComTurmas("PENDENTE", turmaA, turmaB)).getBody());

        ResponseEntity<Void> resposta = client.delete().uri("/turmas/{id}", turmaA)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(atividadeRepository.existsById(atividadeId)).isFalse();
    }

    private static Map<String, Object> atividadeComTurmas(String status, Long... turmaIds) {
        Map<String, Object> atividade = buildAtividade(nomeUnico("Compartilhada"), turmaIds[0], status);
        atividade.remove("turma");
        atividade.put("turmas", java.util.Arrays.stream(turmaIds).map(id -> Map.of("id", id)).toList());
        return atividade;
    }

    private ResponseEntity<Map<String, Object>> criar(Sessao sessao, Map<String, Object> atividade) {
        return client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(atividade)
                .retrieve()
                .toEntity(JSON);
    }

    @SuppressWarnings("unchecked")
    private List<Long> idsDasAtividadesDoAluno(Sessao aluno) {
        List<Map<String, Object>> lista = client.get().uri("/alunos/me/atividades")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .body(LISTA_JSON);
        return lista.stream()
                .map(item -> id((Map<String, Object>) item.get("atividade")))
                .toList();
    }
}

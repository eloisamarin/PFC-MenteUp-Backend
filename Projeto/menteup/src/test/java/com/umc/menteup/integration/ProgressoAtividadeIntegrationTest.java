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

import com.umc.menteup.repository.ProgressoAtividadeRepository;

/**
 * Progresso de atividade: PUT /atividades/{id}/progresso, cruzando turma, atividade,
 * questionário e aluno montados pelos próprios endpoints.
 */
class ProgressoAtividadeIntegrationTest extends IntegracaoBase {

    @Autowired
    private ProgressoAtividadeRepository progressoRepository;

    /** Cenário padrão: atividade publicada, com perguntas, e um aluno matriculado. */
    private record Cenario(Sessao professor, Sessao aluno, Long turmaId, Long atividadeId, List<PerguntaCriada> perguntas) {
    }

    private Cenario cenarioComAlunoMatriculado(int quantidadePerguntas) {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, quantidadePerguntas);
        return new Cenario(professor, aluno, turmaId, atividadeId, perguntas);
    }

    @Test
    @SuppressWarnings("unchecked")
    void salvarProgresso_primeiraResposta_deveRetornar200Com50PorCentoEGravarNoBanco() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(2);
        PerguntaCriada pergunta = c.perguntas().get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("percentualConclusao", 50)
                .containsEntry("respondidas", 1)
                .containsEntry("totalQuestoes", 2);
        assertThat((List<Object>) resposta.getBody().get("respostas")).hasSize(1);
        assertThat(progressoRepository.findByUsuario_IdAndAtividade_Id(c.aluno().id(), c.atividadeId())).isPresent();
    }

    @Test
    void salvarProgresso_todasAsPerguntas_deveRetornar200Com100PorCento() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(2);
        PerguntaCriada primeira = c.perguntas().get(0);
        PerguntaCriada segunda = c.perguntas().get(1);
        responder(c.aluno(), c.atividadeId(), primeira.id(), primeira.alternativas().get(0));

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), segunda.id(), segunda.alternativas().get(1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("percentualConclusao", 100)
                .containsEntry("respondidas", 2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void salvarProgresso_mesmaPerguntaDeNovo_deveTrocarAAlternativaSemDuplicar() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(2);
        PerguntaCriada pergunta = c.perguntas().get(0);
        Long alternativaNova = pergunta.alternativas().get(1);
        responder(c.aluno(), c.atividadeId(), pergunta.id(), pergunta.alternativas().get(0));

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), pergunta.id(), alternativaNova);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("respondidas", 1);
        List<Map<String, Object>> respostas = (List<Map<String, Object>>) resposta.getBody().get("respostas");
        assertThat(respostas).hasSize(1);
        assertThat(((Number) respostas.get(0).get("alternativaId")).longValue()).isEqualTo(alternativaNova);
    }

    @Test
    void salvarProgresso_comAtividadeInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        PerguntaCriada pergunta = c.perguntas().get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), Long.MAX_VALUE, pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENCONTRADA");
    }

    @Test
    void salvarProgresso_comoProfessor_deveRetornar403ComCodigoENaoGravar() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        PerguntaCriada pergunta = c.perguntas().get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.professor(), c.atividadeId(), pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_ALUNO");
        assertThat(progressoRepository.findByUsuario_IdAndAtividade_Id(c.professor().id(), c.atividadeId())).isEmpty();
    }

    @Test
    void salvarProgresso_semToken_deveRetornar401() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        PerguntaCriada pergunta = c.perguntas().get(0);

        // Act
        ResponseEntity<Void> resposta = client.put().uri("/atividades/{id}/progresso", c.atividadeId())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntaId", pergunta.id(), "alternativaId", pergunta.alternativas().get(0)))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void salvarProgresso_semAlternativa_deveRetornar400ComCampoInvalido() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.put()
                .uri("/atividades/{id}/progresso", c.atividadeId())
                .header(HttpHeaders.AUTHORIZATION, c.aluno().token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntaId", c.perguntas().get(0).id()))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos"))
                .containsEntry("respostaInformada", "Informe alternativaId ou alternativasIds");
    }

    @Test
    void salvarProgresso_comPerguntaDeOutraAtividade_deveRetornar400ComCodigo() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        Long outraAtividade = criarAtividade(c.professor(), c.turmaId(), "EM_ANDAMENTO");
        PerguntaCriada perguntaDeFora = cadastrarQuestionario(c.professor(), outraAtividade, 1).get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), perguntaDeFora.id(), perguntaDeFora.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "PERGUNTA_INVALIDA");
    }

    @Test
    void salvarProgresso_comAlternativaDeOutraPergunta_deveRetornar400ComCodigo() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(2);
        PerguntaCriada primeira = c.perguntas().get(0);
        PerguntaCriada segunda = c.perguntas().get(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), primeira.id(), segunda.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALTERNATIVA_INVALIDA");
    }

    @Test
    void salvarProgresso_aposConcluirAAtividade_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        PerguntaCriada pergunta = c.perguntas().get(0);
        responder(c.aluno(), c.atividadeId(), pergunta.id(), pergunta.alternativas().get(0));
        ResponseEntity<Map<String, Object>> conclusao = client.post()
                .uri("/atividades/{id}/respostas", c.atividadeId())
                .header(HttpHeaders.AUTHORIZATION, c.aluno().token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("respostas", List.of(
                        Map.of("perguntaId", pergunta.id(), "alternativaId", pergunta.alternativas().get(0)))))
                .retrieve()
                .toEntity(JSON);
        assertThat(conclusao.getStatusCode().is2xxSuccessful()).isTrue();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.atividadeId(), pergunta.id(), pergunta.alternativas().get(1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_JA_RESPONDIDA");
    }

    @Test
    void salvarProgresso_comAlunoForaDaTurma_deveRetornar403ComCodigoENaoGravar() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        Sessao alunoDeFora = novaSessao("ALUNO");
        PerguntaCriada pergunta = c.perguntas().get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(alunoDeFora, c.atividadeId(), pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_FORA_DA_TURMA");
        assertThat(progressoRepository.findByUsuario_IdAndAtividade_Id(alunoDeFora.id(), c.atividadeId())).isEmpty();
    }

    @Test
    void salvarProgresso_emAtividadeRascunho_deveRetornar404ENaoGravar() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        Long rascunho = criarAtividade(c.professor(), c.turmaId(), "PENDENTE");
        PerguntaCriada pergunta = cadastrarQuestionario(c.professor(), rascunho, 1).get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), rascunho, pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENCONTRADA");
        assertThat(progressoRepository.findByUsuario_IdAndAtividade_Id(c.aluno().id(), rascunho)).isEmpty();
    }

    @Test
    void salvarProgresso_emAtividadeEncerrada_deveRetornar409ENaoGravar() {
        // Arrange
        Cenario c = cenarioComAlunoMatriculado(1);
        Long encerrada = criarAtividade(c.professor(), c.turmaId(), "CONCLUIDO");
        PerguntaCriada pergunta = cadastrarQuestionario(c.professor(), encerrada, 1).get(0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), encerrada, pergunta.id(), pergunta.alternativas().get(0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_ENCERRADA");
        assertThat(progressoRepository.findByUsuario_IdAndAtividade_Id(c.aluno().id(), encerrada)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void fluxoCompleto_turmaAlunoAtividadeEProgresso_deveAparecerNaAtividadeENaListaDoAluno() {
        // Arrange: tudo criado pelos endpoints, cruzando turma, usuário, atividade e questionário
        Cenario c = cenarioComAlunoMatriculado(2);
        PerguntaCriada pergunta = c.perguntas().get(0);
        Long alternativa = pergunta.alternativas().get(0);

        // Act
        responder(c.aluno(), c.atividadeId(), pergunta.id(), alternativa);

        // Assert: o progresso volta na consulta da atividade...
        ResponseEntity<Map<String, Object>> atividade = buscarAtividade(c.aluno(), c.atividadeId());
        assertThat(atividade.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> meuProgresso = (Map<String, Object>) atividade.getBody().get("meuProgresso");
        assertThat(meuProgresso).containsEntry("percentualConclusao", 50)
                .containsEntry("respondidas", 1);

        // ...e na listagem do aluno, com a atividade da turma em andamento
        ResponseEntity<List<Map<String, Object>>> lista = client.get().uri("/alunos/me/atividades")
                .header(HttpHeaders.AUTHORIZATION, c.aluno().token())
                .retrieve()
                .toEntity(LISTA_JSON);
        assertThat(lista.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(lista.getBody()).hasSize(1);
        assertThat(lista.getBody().get(0)).containsEntry("status", "EM_ANDAMENTO")
                .containsEntry("percentualConclusao", 50);
    }
}

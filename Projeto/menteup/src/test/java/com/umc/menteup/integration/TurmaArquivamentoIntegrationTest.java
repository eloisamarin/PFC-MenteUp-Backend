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

import com.umc.menteup.repository.DesafioRepository;
import com.umc.menteup.repository.TurmaRepository;

/*
 * Exclusão e arquivamento de turmas:
 * - a exclusão é recusada com alunos vinculados ou com histórico (desafios, resultados,
 *   atividades em andamento), com uma mensagem que diz exatamente o que prende a turma;
 * - GET /turmas/{id}/exclusao avisa isso antes de tentar;
 * - arquivar tira a turma das listas e impede atividades, desafios e alunos novos,
 *   mantendo o histórico. Desarquivar volta tudo ao normal.
 */
class TurmaArquivamentoIntegrationTest extends IntegracaoBase {

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private DesafioRepository desafioRepository;

    // ------------------------------------------------------------- Verificação antes de excluir

    @Test
    void verificarExclusao_turmaVazia_podeExcluir() {
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        Map<String, Object> analise = verificarExclusao(professor, turmaId).getBody();

        assertThat(analise).containsEntry("podeExcluir", true).containsEntry("alunos", 0);
        assertThat(analise.get("motivo")).isNull();
    }

    @Test
    void verificarExclusao_comAluno_deveDizerQuantosAlunosEstaoVinculados() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        Map<String, Object> analise = verificarExclusao(professor, turmaId).getBody();

        assertThat(analise).containsEntry("podeExcluir", false).containsEntry("alunos", 1);
        assertThat((String) analise.get("motivo")).contains("1 aluno vinculado", "arquive");
    }

    @Test
    void excluir_turmaComHistorico_deveRecusarDizendoOQueAPrende() {
        // Arrange: um desafio e um aluno que começou uma atividade; depois o aluno sai da turma
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        criarDesafio(professor, turmaId);
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        PerguntaCriada pergunta = cadastrarQuestionario(professor, atividadeId, 2).get(0);
        responder(aluno, atividadeId, pergunta.id(), pergunta.alternativas().get(0));
        removerAluno(professor, turmaId, aluno.id());

        // Act
        Map<String, Object> analise = verificarExclusao(professor, turmaId).getBody();
        ResponseEntity<Map<String, Object>> exclusao = excluir(professor, turmaId);

        // Assert: a verificação e a exclusão dizem a mesma coisa, e nada é apagado
        assertThat(analise).containsEntry("podeExcluir", false)
                .containsEntry("desafios", 1)
                .containsEntry("atividadesEmAndamento", 1);
        assertThat((String) analise.get("motivo"))
                .contains("1 desafio", "1 atividade em andamento", "para não apagar os pontos dos alunos");

        assertThat(exclusao.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(exclusao.getBody()).containsEntry("codigo", "TURMA_COM_HISTORICO")
                .containsEntry("mensagem", analise.get("motivo"));
        assertThat(turmaRepository.existsById(turmaId)).isTrue();
    }

    @Test
    void verificarExclusao_deOutroProfessor_deveRetornar403() {
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(dono);

        assertThat(verificarExclusao(outro, turmaId).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    // ------------------------------------------------------------- Arquivar

    @Test
    void arquivar_deveTirarDaListaEManterOHistorico() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        criarDesafio(professor, turmaId);

        // Act
        ResponseEntity<Map<String, Object>> resposta = alterarArquivamento(professor, turmaId, "arquivar");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("arquivada", true);
        assertThat(idsDaLista(professor, false)).doesNotContain(turmaId);
        assertThat(idsDaLista(professor, true)).contains(turmaId);
        assertThat(desafioRepository.countByTurma_Id(turmaId)).isEqualTo(1);
    }

    @Test
    void turmaArquivada_naoDeveReceberAtividadeDesafioNemAluno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        alterarArquivamento(professor, turmaId, "arquivar");

        // Act
        ResponseEntity<Map<String, Object>> atividade = post(professor, "/atividades",
                buildAtividade(nomeUnico("Atividade"), turmaId, "PENDENTE"));
        ResponseEntity<Map<String, Object>> desafio = post(professor, "/desafios", buildDesafio(turmaId));
        ResponseEntity<Map<String, Object>> vinculo = post(professor,
                "/turmas/" + turmaId + "/alunos/" + aluno.id(), null);
        ResponseEntity<Map<String, Object>> pedido = post(aluno, "/alunos/me/solicitacoes",
                Map.of("turmaId", turmaId, "tipo", "ENTRADA"));

        // Assert
        for (ResponseEntity<Map<String, Object>> resposta : List.of(atividade, desafio, vinculo, pedido)) {
            assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_ARQUIVADA");
        }
    }

    @Test
    void turmaArquivada_naoDeveAparecerParaOAlunoPedirEntrada() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        alterarArquivamento(professor, turmaId, "arquivar");

        List<Map<String, Object>> disponiveis = client.get().uri("/alunos/me/turmas/disponiveis")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();

        assertThat(disponiveis).extracting(IntegracaoBase::id).doesNotContain(turmaId);
    }

    @Test
    void desarquivar_deveVoltarParaAListaEAceitarAtividades() {
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        alterarArquivamento(professor, turmaId, "arquivar");

        ResponseEntity<Map<String, Object>> resposta = alterarArquivamento(professor, turmaId, "desarquivar");

        assertThat(resposta.getBody()).containsEntry("arquivada", false);
        assertThat(idsDaLista(professor, false)).contains(turmaId);
        criarAtividade(professor, turmaId, "PENDENTE"); // o helper confere o 201
    }

    @Test
    void arquivar_porAlunoOuOutroProfessor_deveRetornar403() {
        Sessao dono = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(dono);

        assertThat(alterarArquivamento(outro, turmaId, "arquivar").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(alterarArquivamento(aluno, turmaId, "arquivar").getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(turmaRepository.findById(turmaId).orElseThrow().isArquivada()).isFalse();
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> verificarExclusao(Sessao sessao, Long turmaId) {
        return client.get().uri("/turmas/{id}/exclusao", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> excluir(Sessao sessao, Long turmaId) {
        return client.delete().uri("/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> alterarArquivamento(Sessao sessao, Long turmaId, String acao) {
        return client.put().uri("/turmas/{id}/" + acao, turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private void removerAluno(Sessao sessao, Long turmaId, Long alunoId) {
        client.delete().uri("/turmas/{turmaId}/alunos/{alunoId}", turmaId, alunoId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();
    }

    private List<Long> idsDaLista(Sessao sessao, boolean incluirArquivadas) {
        return client.get().uri("/turmas/all?incluirArquivadas=" + incluirArquivadas)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody()
                .stream()
                .map(IntegracaoBase::id)
                .toList();
    }

    private void criarDesafio(Sessao sessao, Long turmaId) {
        assertThat(post(sessao, "/desafios", buildDesafio(turmaId)).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<Map<String, Object>> post(Sessao sessao, String uri, Object corpo) {
        var requisicao = client.post().uri(uri)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON);
        if (corpo != null) {
            requisicao = requisicao.body(corpo);
        }
        return requisicao.retrieve().toEntity(JSON);
    }

    private static Map<String, Object> buildDesafio(Long turmaId) {
        Map<String, Object> pergunta = new HashMap<>();
        pergunta.put("enunciado", "Pergunta 1");
        pergunta.put("tempoSegundos", 30);
        pergunta.put("alternativas", List.of(
                Map.of("texto", "Certa", "correta", true),
                Map.of("texto", "Errada", "correta", false)));

        Map<String, Object> desafio = new HashMap<>();
        desafio.put("titulo", nomeUnico("Desafio"));
        desafio.put("descricao", "Desafio de teste da turma");
        desafio.put("turmaId", turmaId);
        desafio.put("perguntas", List.of(pergunta));
        return desafio;
    }
}

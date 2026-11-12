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

import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.StatusAtividade;
import com.umc.menteup.repository.AtividadeRepository;

class AtividadeControllerIntegrationTest extends IntegracaoBase {

    private static final String BASE_PATH = "/atividades";

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Test
    @SuppressWarnings("unchecked")
    void criar_comDadosValidos_deveRetornar201ComIdETurmaEGravarNoBanco() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        String titulo = nomeUnico("Frações");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, buildAtividade(titulo, turmaId, "PENDENTE"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("titulo", titulo)
                .containsEntry("pontuacao", 10)
                .containsEntry("status", "PENDENTE");
        assertThat(id((Map<String, Object>) resposta.getBody().get("turma"))).isEqualTo(turmaId);

        Atividade salva = atividadeRepository.findById(id(resposta.getBody())).orElseThrow();
        assertThat(salva.getTitulo()).isEqualTo(titulo);
        assertThat(salva.getTurma().getId()).isEqualTo(turmaId);
    }

    @Test
    void criar_comTurmaInexistente_deveRetornar400ComMensagemENaoGravar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        String titulo = nomeUnico("Sem Turma");

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                criar(professor, buildAtividade(titulo, Long.MAX_VALUE, "PENDENTE"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "Turma não existe!");
        assertThat(atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void criar_comTituloCurto_deveRetornar400ComCampoInvalido() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, buildAtividade("Oi", turmaId, "PENDENTE"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos")).containsKey("titulo");
    }

    @Test
    void criar_semTurma_deveRetornar400ENaoErroInterno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> semTurma = buildAtividade(nomeUnico("Sem Turma"), 1L, "PENDENTE");
        semTurma.remove("turma");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, semTurma);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "A turma da atividade é obrigatória.");
    }

    @Test
    void criar_semToken_deveRetornar401ENaoGravar() {
        // Arrange
        String titulo = nomeUnico("Sem Token");

        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(titulo, 1L, "PENDENTE"))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(atividadeRepository.findAllByTituloContainingIgnoreCase(titulo)).isEmpty();
    }

    @Test
    void listarTodas_comToken_deveRetornar200ComAtividadeCriada() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "PENDENTE");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(IntegracaoBase::id).contains(atividadeId);
    }

    @Test
    void buscarPorTitulo_ignorandoMaiusculas_deveRetornar200ComAAtividade() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        String titulo = nomeUnico("Geometria");
        criar(professor, buildAtividade(titulo, criarTurma(professor), "PENDENTE"));

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = buscarPorTitulo(professor, titulo.toUpperCase());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(atividade -> atividade.get("titulo")).containsExactly(titulo);
    }

    @Test
    void buscarPorTitulo_semResultado_deveRetornar200ComListaVazia() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = buscarPorTitulo(professor, nomeUnico("Inexistente"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPorId_comoProfessor_deveRetornar200ComRascunhoEPerguntas() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "PENDENTE");
        cadastrarQuestionario(professor, atividadeId, 2);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(professor, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<String, Object> atividade = (Map<String, Object>) resposta.getBody().get("atividade");
        assertThat(id(atividade)).isEqualTo(atividadeId);
        assertThat(atividade).containsEntry("status", "PENDENTE");
        assertThat((List<Object>) resposta.getBody().get("perguntas")).hasSize(2);
    }

    @Test
    void buscarPorId_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(professor, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENCONTRADA");
    }

    @Test
    void buscarPorId_comAlunoForaDaTurma_deveRetornar403ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao alunoDeFora = novaSessao("ALUNO");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "EM_ANDAMENTO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(alunoDeFora, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_FORA_DA_TURMA");
    }

    @Test
    void buscarPorId_comAlunoDaTurmaEmRascunho_deveRetornar404() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(aluno, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ATIVIDADE_NAO_ENCONTRADA");
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPorId_comAlunoDaTurmaEmPublicada_deveRetornar200SemExporGabarito() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        cadastrarQuestionario(professor, atividadeId, 1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(aluno, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> perguntas = (List<Map<String, Object>>) resposta.getBody().get("perguntas");
        assertThat(perguntas).hasSize(1);
        List<Map<String, Object>> alternativas = (List<Map<String, Object>>) perguntas.get(0).get("alternativas");
        assertThat(alternativas).hasSize(2)
                .allSatisfy(alternativa -> assertThat(alternativa).doesNotContainKey("correta"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscarPorId_comProfessor_deveRetornar200ComGabarito() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        cadastrarQuestionario(professor, atividadeId, 1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarAtividade(professor, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        List<Map<String, Object>> perguntas = (List<Map<String, Object>>) resposta.getBody().get("perguntas");
        List<Map<String, Object>> alternativas = (List<Map<String, Object>>) perguntas.get(0).get("alternativas");
        assertThat(alternativas).hasSize(2).allSatisfy(alternativa -> assertThat(alternativa).containsKey("correta"));
    }

    @Test
    void atualizar_dePendenteParaEmAndamento_deveRetornar200EGravarOStatusNovo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");
        String novoTitulo = nomeUnico("Publicada");
        Map<String, Object> dados = buildAtividade(novoTitulo, turmaId, "EM_ANDAMENTO");
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("titulo", novoTitulo)
                .containsEntry("status", "EM_ANDAMENTO");

        Atividade salva = atividadeRepository.findById(atividadeId).orElseThrow();
        assertThat(salva.getStatus()).isEqualTo(StatusAtividade.EM_ANDAMENTO);
        assertThat(salva.getTitulo()).isEqualTo(novoTitulo);
    }

    @Test
    @SuppressWarnings("unchecked")
    void atualizar_atividadeComQuestionario_devePreservarAsPerguntas() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");
        cadastrarQuestionario(professor, atividadeId, 2);
        Map<String, Object> dados = buildAtividade(nomeUnico("Editada"), turmaId, "PENDENTE");
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<Object>) buscarAtividade(professor, atividadeId).getBody().get("perguntas")).hasSize(2);
    }

    @Test
    void atualizar_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> dados = buildAtividade(nomeUnico("Fantasma"), criarTurma(professor), "PENDENTE");
        dados.put("id", Long.MAX_VALUE);

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "NOT_FOUND");
    }

    @Test
    void atualizar_semId_deveRetornar400ENaoErroInterno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> dados = new HashMap<>(
                buildAtividade(nomeUnico("Sem Id"), criarTurma(professor), "PENDENTE"));

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "O id da atividade é obrigatório para atualizar.");
    }

    @Test
    void atualizar_semTurma_deveRetornar400ENaoAlterar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");
        Map<String, Object> dados = buildAtividade(nomeUnico("Sem Turma"), turmaId, "PENDENTE");
        dados.put("id", atividadeId);
        dados.remove("turma");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "A turma da atividade é obrigatória.");
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTurma().getId()).isEqualTo(turmaId);
    }

    @Test
    void atualizar_comTurmaInexistente_deveRetornar400ENaoAlterar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");
        Map<String, Object> dados = buildAtividade(nomeUnico("Turma Errada"), Long.MAX_VALUE, "PENDENTE");
        dados.put("id", atividadeId);

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "Turma não existe");
        assertThat(atividadeRepository.findById(atividadeId).orElseThrow().getTurma().getId()).isEqualTo(turmaId);
    }

    @Test
    void excluir_comIdExistente_deveRetornar204EAtividadeDeixaDeExistir() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "PENDENTE");
        cadastrarQuestionario(professor, atividadeId, 1);

        // Act
        ResponseEntity<Void> resposta = excluir(professor, atividadeId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(buscarAtividade(professor, atividadeId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(atividadeRepository.findById(atividadeId)).isEmpty();
    }

    @Test
    void excluir_atividadeComProgressoDeAluno_deveRetornar409EManterOsDados() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        PerguntaCriada pergunta = cadastrarQuestionario(professor, atividadeId, 1).get(0);
        responder(aluno, atividadeId, pergunta.id(), pergunta.alternativas().get(0));

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.delete().uri(BASE_PATH + "/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "CONFLITO_DE_DADOS");
        assertThat(atividadeRepository.findById(atividadeId)).isPresent();
    }

    @Test
    void excluir_comIdInexistente_deveRetornar404() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Void> resposta = excluir(professor, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void excluir_semToken_deveRetornar401EAtividadeContinuaExistindo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long atividadeId = criarAtividade(professor, criarTurma(professor), "PENDENTE");

        // Act
        ResponseEntity<Void> resposta = client.delete().uri(BASE_PATH + "/{id}", atividadeId)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(atividadeRepository.findById(atividadeId)).isPresent();
    }

    private ResponseEntity<Map<String, Object>> criar(Sessao sessao, Map<String, Object> atividade) {
        return client.post().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(atividade)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao sessao, Map<String, Object> atividade) {
        return client.put().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(atividade)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<List<Map<String, Object>>> buscarPorTitulo(Sessao sessao, String titulo) {
        return client.get().uri(BASE_PATH + "/titulo/{titulo}", titulo)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    private ResponseEntity<Void> excluir(Sessao sessao, Long atividadeId) {
        return client.delete().uri(BASE_PATH + "/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();
    }
}

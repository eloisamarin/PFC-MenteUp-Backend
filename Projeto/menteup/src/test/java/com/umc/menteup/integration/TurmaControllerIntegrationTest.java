package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;

class TurmaControllerIntegrationTest extends IntegracaoBase {

    private static final String BASE_PATH = "/turmas";

    @Autowired
    private TurmaRepository turmaRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Test
    void criar_comoProfessor_deveRetornar201ComIdEGravarNoBanco() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        String nome = nomeUnico("Turma de Java");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, nome);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("nome", nome);
        assertThat(turmaRepository.findById(id(resposta.getBody())).orElseThrow().getNome()).isEqualTo(nome);
    }

    @Test
    void criar_comoAdministrador_deveRetornar201() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String nome = nomeUnico("Turma da Administração");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(administrador, nome);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("nome", nome);
    }

    @Test
    void criar_comoAluno_deveRetornar403ComCodigoENaoGravar() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        String nome = nomeUnico("Turma Proibida");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(aluno, nome);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody())
                .containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR")
                .containsEntry("mensagem", "Esta operação é exclusiva para professores e administradores.");
        assertThat(turmaRepository.findAllByNomeContainingIgnoreCase(nome)).isEmpty();
    }

    @Test
    void criar_semToken_deveRetornar401ENaoGravar() {
        // Arrange
        String nome = nomeUnico("Turma Sem Token");

        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(turmaRepository.findAllByNomeContainingIgnoreCase(nome)).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void criar_comNomeEmBranco_deveRetornar400ComCampoInvalido() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, " ");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos"))
                .containsEntry("nome", "O Atributo Nome da Turma é obrigatório");
    }

    @Test
    void criar_comOrigemDoFrontend_deveRetornarHeaderCors() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nomeUnico("Turma CORS")))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isEqualTo("*");
    }

    @Test
    void preflightCors_semToken_deveLiberarOMetodoParaOFrontend() {
        // Act
        ResponseEntity<Void> resposta = client.method(HttpMethod.OPTIONS).uri(BASE_PATH)
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getAccessControlAllowMethods()).contains(HttpMethod.POST);
    }

    @Test
    void buscarPorId_comIdExistente_deveRetornar200ComOsDados() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscar(professor, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(id(resposta.getBody())).isEqualTo(turmaId);
        assertThat(resposta.getBody().get("nome")).asString().startsWith("Turma");
    }

    @Test
    void buscarPorId_comIdInexistente_deveRetornar404() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscar(professor, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void listarTodas_comToken_deveRetornar200ComTurmaCriada() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri(BASE_PATH + "/all")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(IntegracaoBase::id).contains(turmaId);
    }

    @Test
    void listarTodas_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri(BASE_PATH + "/all")
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void buscarPorNome_ignorandoMaiusculas_deveRetornar200ComATurma() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        String nome = nomeUnico("Biologia");
        criar(professor, nome);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = buscarPorNome(professor, nome.toUpperCase());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(turma -> turma.get("nome")).containsExactly(nome);
    }

    @Test
    void buscarPorNome_semResultado_deveRetornar200ComListaVazia() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = buscarPorNome(professor, nomeUnico("Inexistente"));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void vincularAluno_comoProfessor_deveRetornar204EMatricularNoBanco() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(professor, turmaId, aluno.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(turmaRepository.contarMatricula(turmaId, aluno.id())).isEqualTo(1);

        List<Map<String, Object>> alunos =
                (List<Map<String, Object>>) buscar(professor, turmaId).getBody().get("alunos");
        assertThat(alunos).extracting(matriculado -> matriculado.get("usuario")).containsExactly(aluno.email());
    }

    @Test
    void vincularAluno_duasVezes_deveRetornar409ComCodigoESemDuplicar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(professor, turmaId, aluno.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_JA_VINCULADO");
        assertThat(turmaRepository.contarMatricula(turmaId, aluno.id())).isEqualTo(1);
    }

    @Test
    void vincularAluno_comUsuarioQueNaoEAluno_deveRetornar400ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(professor, turmaId, outroProfessor.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "USUARIO_NAO_E_ALUNO");
        assertThat(turmaRepository.contarMatricula(turmaId, outroProfessor.id())).isZero();
    }

    @Test
    void vincularAluno_comTurmaInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(professor, Long.MAX_VALUE, aluno.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void vincularAluno_comAlunoInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(professor, turmaId, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_NAO_ENCONTRADO");
    }

    @Test
    void vincularAluno_comoAluno_deveRetornar403ENaoMatricular() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = vincular(aluno, turmaId, aluno.id());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(turmaRepository.contarMatricula(turmaId, aluno.id())).isZero();
    }

    @Test
    void atualizar_comoProfessor_deveRetornar200EManterOsAlunos() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        String novoNome = nomeUnico("Nome Novo");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, turmaId, novoNome);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("nome", novoNome);
        assertThat(turmaRepository.findById(turmaId).orElseThrow().getNome()).isEqualTo(novoNome);
        assertThat(turmaRepository.contarMatricula(turmaId, aluno.id())).isEqualTo(1);
    }

    @Test
    void atualizar_semId_deveRetornar400ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, null, "Sem Id");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ID_TURMA_OBRIGATORIO");
    }

    @Test
    void atualizar_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, Long.MAX_VALUE, "Fantasma");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void atualizar_comoAluno_deveRetornar403ENaoAlterarONome() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        String nomeOriginal = turmaRepository.findById(turmaId).orElseThrow().getNome();

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(aluno, turmaId, "Alterada pelo aluno");

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(turmaRepository.findById(turmaId).orElseThrow().getNome()).isEqualTo(nomeOriginal);
    }

    @Test
    void excluir_comoAdministrador_deveRetornar204ETurmaDeixaDeExistir() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Long turmaId = criarTurma(administrador);

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(administrador, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(buscar(administrador, turmaId).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void excluir_turmaComAtividadeEAluno_deveRemoverAAtividadeEManterOAluno() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "PENDENTE");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(professor, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(atividadeRepository.findById(atividadeId)).isEmpty();
        assertThat(turmaRepository.findAllByAlunos_Id(aluno.id())).isEmpty();

        ResponseEntity<Void> alunoAposExclusao = client.get().uri("/usuarios/{id}", aluno.id())
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toBodilessEntity();
        assertThat(alunoAposExclusao.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void excluir_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(administrador, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void excluir_comoAluno_deveRetornar403ETurmaContinuaExistindo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(aluno, turmaId);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(turmaRepository.findById(turmaId)).isPresent();
    }

    private ResponseEntity<Map<String, Object>> criar(Sessao sessao, String nome) {
        return client.post().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nome))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> buscar(Sessao sessao, Long turmaId) {
        return client.get().uri(BASE_PATH + "/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<List<Map<String, Object>>> buscarPorNome(Sessao sessao, String nome) {
        return client.get().uri(BASE_PATH + "/nome/{nome}", nome)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    private ResponseEntity<Map<String, Object>> vincular(Sessao sessao, Long turmaId, Long alunoId) {
        return client.post().uri(BASE_PATH + "/{turmaId}/alunos/{alunoId}", turmaId, alunoId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao sessao, Long turmaId, String nome) {
        Map<String, Object> dados = new HashMap<>();
        dados.put("id", turmaId);
        dados.put("nome", nome);

        return client.put().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> excluir(Sessao sessao, Long turmaId) {
        return client.delete().uri(BASE_PATH + "/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }
}

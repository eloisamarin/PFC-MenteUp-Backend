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

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.UsuarioRepository;

/*
 * LGPD (minimização): as respostas só trazem os dados pessoais necessários.
 * - /usuarios/all: o aluno recebe só id, nome e tipo; o professor também o e-mail dos alunos
 *   (para adicioná-los na turma); o administrador, o e-mail de todos;
 * - /usuarios/{id}: só o próprio usuário ou o administrador;
 * - turmas e atividades não expõem e-mail nem token do celular (FCM);
 * - o autor da atividade vem do token, não do corpo da requisição.
 */
class DadosPessoaisIntegrationTest extends IntegracaoBase {

    private static final List<String> CAMPOS_RESUMIDOS = List.of("id", "nomeUsuario", "tipoUsuario");

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Test
    void listarUsuarios_comoAluno_deveTrazerSoIdNomeETipo() {
        Sessao aluno = novaSessao("ALUNO");
        novaSessao("PROFESSOR");

        List<Map<String, Object>> usuarios = listar(aluno);

        assertThat(usuarios).isNotEmpty();
        assertThat(usuarios).allSatisfy(u -> assertThat(u.keySet()).containsOnlyElementsOf(CAMPOS_RESUMIDOS));
    }

    @Test
    void listarUsuarios_comoProfessor_deveTrazerOEmailSoDosAlunos() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Sessao outroProfessor = novaSessao("PROFESSOR");
        gravarTokenDoCelular(aluno.id());

        // Act
        List<Map<String, Object>> usuarios = listar(professor);

        // Assert: e-mail do aluno (para adicioná-lo na turma); dos outros perfis, não
        assertThat(usuarios).filteredOn(u -> aluno.id().equals(id(u))).singleElement()
                .satisfies(u -> assertThat(u).containsEntry("usuario", aluno.email())
                        .doesNotContainKeys("fcmToken", "aguardandoPrimeiroAcesso", "turma", "atividade"));
        assertThat(usuarios).filteredOn(u -> outroProfessor.id().equals(id(u))).singleElement()
                .satisfies(u -> assertThat(u.keySet()).containsOnlyElementsOf(CAMPOS_RESUMIDOS));
        assertThat(usuarios).filteredOn(u -> !"ALUNO".equals(u.get("tipoUsuario")))
                .allSatisfy(u -> assertThat(u).doesNotContainKey("usuario"));
    }

    @Test
    void listarUsuarios_comoAdministrador_deveTrazerOEmailMasNaoOToken() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");
        gravarTokenDoCelular(aluno.id());

        Map<String, Object> doAluno = listar(administrador).stream()
                .filter(u -> aluno.id().equals(id(u)))
                .findFirst()
                .orElseThrow();

        assertThat(doAluno).containsEntry("usuario", aluno.email())
                .doesNotContainKeys("fcmToken", "senha", "turma", "atividade");
    }

    @Test
    void buscarUsuarioPorId_deOutraPessoa_deveRetornar403() {
        Sessao aluno = novaSessao("ALUNO");
        Sessao professor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> resposta = buscarUsuario(professor, aluno.id());

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROPRIO_USUARIO");
    }

    @Test
    void buscarUsuarioPorId_proprioOuAdministrador_deveRetornar200() {
        Sessao aluno = novaSessao("ALUNO");
        Sessao administrador = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> proprio = buscarUsuario(aluno, aluno.id());
        ResponseEntity<Map<String, Object>> peloAdmin = buscarUsuario(administrador, aluno.id());

        assertThat(proprio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(proprio.getBody()).containsEntry("usuario", aluno.email()).doesNotContainKey("fcmToken");
        assertThat(peloAdmin.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void turma_naoDeveExporEmailNemTokenDosAlunos() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        gravarTokenDoCelular(aluno.id());
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());

        // Act
        Map<String, Object> turma = client.get().uri("/turmas/{id}", turmaId)
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toEntity(JSON)
                .getBody();
        List<Map<String, Object>> todas = client.get().uri("/turmas/all")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();

        // Assert
        List<Map<String, Object>> alunos = (List<Map<String, Object>>) turma.get("alunos");
        assertThat(alunos).singleElement()
                .satisfies(a -> assertThat(a.keySet()).containsOnlyElementsOf(CAMPOS_RESUMIDOS));
        assertThat(todas).allSatisfy(t -> ((List<Map<String, Object>>) t.get("alunos"))
                .forEach(a -> assertThat(a.keySet()).containsOnlyElementsOf(CAMPOS_RESUMIDOS)));
    }

    @Test
    void atividade_naoDeveExporOAutorEOAutorDeveVirDoToken() {
        // Arrange: tenta dizer no corpo que o autor é outra pessoa
        Sessao professor = novaSessao("PROFESSOR");
        Sessao outro = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);
        Map<String, Object> corpo = new java.util.HashMap<>(
                buildAtividade(nomeUnico("Atividade"), turmaId, "PENDENTE"));
        corpo.put("usuario", Map.of("id", outro.id()));

        // Act
        ResponseEntity<Map<String, Object>> criada = client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);
        List<Map<String, Object>> lista = client.get().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();

        // Assert
        assertThat(criada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(criada.getBody()).doesNotContainKey("usuario");
        assertThat(lista).allSatisfy(a -> assertThat(a).doesNotContainKey("usuario"));
        Usuario autor = atividadeRepository.findById(id(criada.getBody())).orElseThrow().getUsuario();
        assertThat(autor.getId()).isEqualTo(professor.id());
    }

    // ------------------------------------------------------------- Helpers

    private List<Map<String, Object>> listar(Sessao sessao) {
        return client.get().uri("/usuarios/all")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON)
                .getBody();
    }

    private ResponseEntity<Map<String, Object>> buscarUsuario(Sessao sessao, Long id) {
        return client.get().uri("/usuarios/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private void gravarTokenDoCelular(Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        usuario.setFcmToken("token-secreto-do-celular");
        usuarioRepository.save(usuario);
    }
}

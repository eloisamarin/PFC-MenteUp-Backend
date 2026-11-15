package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.repository.LogSistemaRepository;

// "Baixar meus dados": a pessoa recebe tudo o que é dela, e nada de outra pessoa nem segredos da conta
class ExportacaoDadosIntegrationTest extends IntegracaoBase {

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Test
    @SuppressWarnings("unchecked")
    void exportar_deveTrazerOsDadosDaPropriaPessoa() {
        // Arrange: aluno com uma atividade concluída numa turma
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        concluirAtividade(aluno, atividadeId, cadastrarQuestionario(professor, atividadeId, 1), true);

        // Act
        ResponseEntity<Map<String, Object>> resposta = exportar(aluno);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).startsWith("attachment");

        Map<String, Object> dados = resposta.getBody();
        assertThat((Map<String, Object>) dados.get("conta"))
                .containsEntry("email", aluno.email())
                .containsEntry("tipo", "ALUNO");
        assertThat((List<?>) dados.get("aceites")).hasSize(2);
        assertThat((List<Map<String, Object>>) dados.get("turmas")).extracting(IntegracaoBase::id)
                .containsExactly(turmaId);
        assertThat((List<?>) dados.get("atividades")).hasSize(1);
        assertThat((List<?>) dados.get("registros")).isNotEmpty();
    }

    @Test
    void exportar_naoDeveLevarSegredosDaConta() {
        Sessao aluno = novaSessao("ALUNO");

        String json = client.get().uri("/usuarios/me/exportar")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .body(String.class);

        assertThat(json).doesNotContain("\"senha\"", SENHA, "fcmToken", "codigo2FA", "tokenHash");
    }

    @Test
    void exportar_deveFicarRegistradoNoLog() {
        Sessao aluno = novaSessao("ALUNO");

        exportar(aluno);

        assertThat(logSistemaRepository.findAllByUsuario_IdOrderByDataHoraDesc(aluno.id()))
                .anyMatch(l -> l.getAcao() == AcaoLog.EXPORTAR_DADOS);
    }

    @Test
    void exportar_semToken_deveRetornar401() {
        ResponseEntity<Void> resposta = client.get().uri("/usuarios/me/exportar")
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private ResponseEntity<Map<String, Object>> exportar(Sessao sessao) {
        return client.get().uri("/usuarios/me/exportar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }
}

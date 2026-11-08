package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

///Testa as conquistas pelas rotas reais: login, atividade, consulta e permissões
///As de ranking ficam de fora: o banco de teste é compartilhado e a posição varia
class ConquistaIntegrationTest extends IntegracaoBase {

    private record Cenario(Sessao professor, Sessao aluno, Long atividadeId, List<PerguntaCriada> perguntas) {
    }

    private Cenario cenario() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        Long atividadeId = criarAtividade(professor, turmaId, "EM_ANDAMENTO");
        List<PerguntaCriada> perguntas = cadastrarQuestionario(professor, atividadeId, 2);
        return new Cenario(professor, aluno, atividadeId, perguntas);
    }

    private void concluirComTudoCerto(Cenario c) {
        for (PerguntaCriada p : c.perguntas()) {
            responder(c.aluno(), c.atividadeId(), p.id(), p.alternativas().get(0));
        }
        ResponseEntity<Map<String, Object>> conclusao = client.post()
                .uri("/atividades/{id}/respostas", c.atividadeId())
                .header(HttpHeaders.AUTHORIZATION, c.aluno().token())
                .retrieve()
                .toEntity(JSON);
        assertThat(conclusao.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<Map<String, Object>> minhasConquistas(Sessao sessao) {
        return client.get().uri("/alunos/me/conquistas")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<List<Map<String, Object>>> consumirNovas(Sessao sessao) {
        return client.post().uri("/alunos/me/conquistas/novas")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);
    }

    @SuppressWarnings("unchecked")
    private static List<String> codigosDesbloqueados(Map<String, Object> resumo) {
        return ((List<Map<String, Object>>) resumo.get("conquistas")).stream()
                .filter(c -> Boolean.TRUE.equals(c.get("desbloqueada")))
                .map(c -> (String) c.get("codigo"))
                .toList();
    }

    @Test
    void loginDoAluno_deveContarUmDiaDeIntensivaoSemDesbloquearNada() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = minhasConquistas(aluno);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody())
                .containsEntry("diasConsecutivos", 1)
                .containsEntry("xpTotal", 0)
                .containsEntry("conquistasDesbloqueadas", 0)
                .containsEntry("totalConquistas", 18);
    }

    @Test
    void doisLoginsNoMesmoDia_naoDevemContarDoisDias() {
        Sessao aluno = novaSessao("ALUNO");
        autenticar(aluno.email());

        assertThat(minhasConquistas(aluno).getBody()).containsEntry("diasConsecutivos", 1);
    }

    @Test
    void concluirAtividadeComTudoCerto_deveDesbloquearPrimeiraAtividadePerfeicaoENivelInicial() {
        Cenario c = cenario();

        concluirComTudoCerto(c);

        Map<String, Object> resumo = minhasConquistas(c.aluno()).getBody();
        assertThat(codigosDesbloqueados(resumo))
                .contains("PRIMEIRA_ATIVIDADE", "ATIVIDADE_PERFEITA", "NIVEL_INICIANTE")
                .doesNotContain("DEZ_ATIVIDADES", "NIVEL_INTERMEDIARIO");
        ///10 da atividade + 20 + 40 + 20 das conquistas (ranking pode somar mais)
        assertThat(((Number) resumo.get("xpTotal")).longValue()).isGreaterThanOrEqualTo(90);
    }

    @Test
    void novas_deveDevolverOsDesbloqueiosUmaVezSo() {
        Cenario c = cenario();
        concluirComTudoCerto(c);

        ResponseEntity<List<Map<String, Object>>> primeira = consumirNovas(c.aluno());
        ResponseEntity<List<Map<String, Object>>> segunda = consumirNovas(c.aluno());

        assertThat(primeira.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(primeira.getBody()).extracting(m -> m.get("codigo"))
                .contains("PRIMEIRA_ATIVIDADE", "ATIVIDADE_PERFEITA", "NIVEL_INICIANTE");
        assertThat(segunda.getBody()).isEmpty();
    }

    @Test
    void professorDaTurma_podeVerAsConquistasDoAluno_outroProfessorNao() {
        Cenario c = cenario();
        Sessao outroProfessor = novaSessao("PROFESSOR");

        ResponseEntity<Map<String, Object>> doProfessor = client.get()
                .uri("/conquistas/usuario/{id}", c.aluno().id())
                .header(HttpHeaders.AUTHORIZATION, c.professor().token())
                .retrieve()
                .toEntity(JSON);
        ResponseEntity<Map<String, Object>> doOutro = client.get()
                .uri("/conquistas/usuario/{id}", c.aluno().id())
                .header(HttpHeaders.AUTHORIZATION, outroProfessor.token())
                .retrieve()
                .toEntity(JSON);

        assertThat(doProfessor.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(doOutro.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void professor_naoAcessaRotasDoAluno() {
        Sessao professor = novaSessao("PROFESSOR");

        assertThat(minhasConquistas(professor).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void catalogo_deveListarAs18Conquistas() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri("/conquistas")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .retrieve()
                .toEntity(LISTA_JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).hasSize(18);
    }
}

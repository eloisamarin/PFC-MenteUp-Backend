package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.Desafio;
import com.umc.menteup.model.StatusDesafio;
import com.umc.menteup.repository.DesafioRepository;
import com.umc.menteup.repository.RespostaDesafioRepository;

class DesafioControllerIntegrationTest extends IntegracaoBase {

    private static final String BASE_PATH = "/desafios";

    @Autowired
    private DesafioRepository desafioRepository;

    @Autowired
    private RespostaDesafioRepository respostaRepository;

    /** Professor, turma com dois alunos e um desafio ainda aguardando o início. */
    private record Cenario(Sessao professor, Sessao aluno, Sessao colega, Long turmaId, Long desafioId) {
    }

    private Cenario cenario(int quantidadePerguntas) {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Sessao colega = novaSessao("ALUNO");
        Long turmaId = criarTurma(professor);
        vincularAluno(professor, turmaId, aluno.id());
        vincularAluno(professor, turmaId, colega.id());

        ResponseEntity<Map<String, Object>> criado = criar(professor, buildDesafio(turmaId, quantidadePerguntas));
        assertThat(criado.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        return new Cenario(professor, aluno, colega, turmaId, id(criado.getBody()));
    }

    // ------------------------------------------------------------ Criar e excluir

    @Test
    @SuppressWarnings("unchecked")
    void criar_comoProfessor_deveRetornar201AguardandoEGravarNoBanco() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Long turmaId = criarTurma(professor);

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, buildDesafio(turmaId, 2));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("status", "AGUARDANDO")
                .containsEntry("totalPerguntas", 2);
        assertThat(id((Map<String, Object>) resposta.getBody().get("turma"))).isEqualTo(turmaId);
        assertThat(desafioRepository.findById(id(resposta.getBody()))).isPresent();
    }

    @Test
    void criar_comoAdministrador_deveRetornar201() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Long turmaId = criarTurma(administrador);

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(administrador, buildDesafio(turmaId, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("status", "AGUARDANDO");
    }

    @Test
    void criar_comoAluno_deveRetornar403ComCodigoENaoGravar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        long totalAntes = desafioRepository.count();

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(aluno, buildDesafio(criarTurma(professor), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(desafioRepository.count()).isEqualTo(totalAntes);
    }

    @Test
    void criar_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildDesafio(1L, 1))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @SuppressWarnings("unchecked")
    void criar_comTempoAbaixoDoMinimo_deveRetornar400ComCampoInvalido() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> corpo = buildDesafio(criarTurma(professor), 1);
        ((List<Map<String, Object>>) corpo.get("perguntas")).get(0).put("tempoSegundos", 4);

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos"))
                .containsValue("O tempo mínimo é de 5 segundos");
    }

    @Test
    @SuppressWarnings("unchecked")
    void criar_semAlternativaCorreta_deveRetornar400ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Map<String, Object> corpo = buildDesafio(criarTurma(professor), 1);
        ((List<Map<String, Object>>) corpo.get("perguntas")).get(0).put("alternativas", List.of(
                Map.of("texto", "Errada 1", "correta", false),
                Map.of("texto", "Errada 2", "correta", false)));

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, corpo);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALTERNATIVA_CORRETA_INVALIDA");
    }

    @Test
    void criar_comTurmaInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criar(professor, buildDesafio(Long.MAX_VALUE, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void excluir_comoProfessor_deveRetornar204EDesafioDeixaDeExistir() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(desafioRepository.findById(c.desafioId())).isEmpty();
    }

    @Test
    void excluir_comoAluno_deveRetornar403EDesafioContinuaExistindo() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(c.aluno(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(desafioRepository.findById(c.desafioId())).isPresent();
    }

    @Test
    void excluir_desafioEmAndamento_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        iniciar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_EM_ANDAMENTO");
    }

    @Test
    void excluir_desafioEncerradoComRespostas_deveRetornar204EApagarAsRespostas() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));
        responder(c.colega(), c.desafioId(), id(pergunta), alternativa(pergunta, 1));
        avancar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(desafioRepository.findById(c.desafioId())).isEmpty();
        assertThat(respostaRepository.buscarTodasDoDesafio(c.desafioId())).isEmpty();
    }

    @Test
    void excluir_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");

        // Act
        ResponseEntity<Map<String, Object>> resposta = excluir(professor, Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_ENCONTRADO");
    }

    // -------------------------------------------------------------------- Editar

    @Test
    @SuppressWarnings("unchecked")
    void buscarParaEdicao_comoProfessor_deveRetornarDadosComGabarito() {
        // Arrange
        Cenario c = cenario(2);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarParaEdicao(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Number) resposta.getBody().get("turmaId")).longValue()).isEqualTo(c.turmaId());
        List<Map<String, Object>> perguntas = (List<Map<String, Object>>) resposta.getBody().get("perguntas");
        assertThat(perguntas).hasSize(2);
        assertThat(perguntas.get(0)).containsEntry("enunciado", "Pergunta 1")
                .containsEntry("tempoSegundos", 30);
        assertThat((List<Map<String, Object>>) perguntas.get(0).get("alternativas"))
                .extracting(alternativa -> alternativa.get("texto"), alternativa -> alternativa.get("correta"))
                .containsExactly(tuple("Certa 1", true), tuple("Errada 1", false));
    }

    @Test
    void buscarParaEdicao_comoAluno_deveRetornar403SemExporOGabarito() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarParaEdicao(c.aluno(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR")
                .doesNotContainKey("perguntas");
    }

    @Test
    void buscarParaEdicao_desafioIniciado_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        iniciar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = buscarParaEdicao(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_EDITAVEL");
    }

    @Test
    @SuppressWarnings("unchecked")
    void atualizar_comoProfessor_deveRetornar200ESubstituirDadosEPerguntas() {
        // Arrange
        Cenario c = cenario(1);
        Long outraTurma = criarTurma(c.professor());
        Map<String, Object> alteracao = buildDesafio(outraTurma, 3);
        alteracao.put("titulo", "Título editado");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(c.professor(), c.desafioId(), alteracao);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("titulo", "Título editado")
                .containsEntry("status", "AGUARDANDO")
                .containsEntry("totalPerguntas", 3);
        assertThat(id((Map<String, Object>) resposta.getBody().get("turma"))).isEqualTo(outraTurma);

        Map<String, Object> gravado = buscarParaEdicao(c.professor(), c.desafioId()).getBody();
        assertThat((List<Map<String, Object>>) gravado.get("perguntas"))
                .extracting(pergunta -> pergunta.get("enunciado"))
                .containsExactly("Pergunta 1", "Pergunta 2", "Pergunta 3");
    }

    @Test
    void atualizar_comoAdministrador_deveRetornar200() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(novaSessao("ADMINISTRADOR"), c.desafioId(), buildDesafio(c.turmaId(), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void atualizar_eDepoisIniciar_deveMostrarAsPerguntasNovas() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> alteracao = buildDesafio(c.turmaId(), 2);
        atualizar(c.professor(), c.desafioId(), alteracao);

        // Act
        ResponseEntity<Map<String, Object>> resposta = iniciar(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("totalPerguntas", 2);
    }

    @Test
    void atualizar_comoAluno_deveRetornar403ENaoAlterar() {
        // Arrange
        Cenario c = cenario(1);
        String tituloOriginal = desafioRepository.findById(c.desafioId()).orElseThrow().getTitulo();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(c.aluno(), c.desafioId(), buildDesafio(c.turmaId(), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(desafioRepository.findById(c.desafioId()).orElseThrow().getTitulo()).isEqualTo(tituloOriginal);
    }

    @Test
    void atualizar_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.put().uri(BASE_PATH + "/{id}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildDesafio(1L, 1))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void atualizar_desafioEmAndamentoComRespostas_deveRetornar409EManterRespostasEDados() {
        // Arrange
        Cenario c = cenario(2);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));
        String tituloOriginal = desafioRepository.findById(c.desafioId()).orElseThrow().getTitulo();

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(c.professor(), c.desafioId(), buildDesafio(c.turmaId(), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_EDITAVEL");
        assertThat(desafioRepository.findById(c.desafioId()).orElseThrow().getTitulo()).isEqualTo(tituloOriginal);
        assertThat(respostaRepository.buscarTodasDoDesafio(c.desafioId())).hasSize(1);
    }

    @Test
    void atualizar_desafioEncerrado_deveRetornar409() {
        // Arrange
        Cenario c = cenario(1);
        iniciar(c.professor(), c.desafioId());
        esgotarTempo(c.desafioId());
        avancar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(c.professor(), c.desafioId(), buildDesafio(c.turmaId(), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_EDITAVEL");
    }

    @Test
    void atualizar_comIdInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(c.professor(), Long.MAX_VALUE, buildDesafio(c.turmaId(), 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_ENCONTRADO");
    }

    @Test
    void atualizar_comTurmaInexistente_deveRetornar404ComCodigo() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                atualizar(c.professor(), c.desafioId(), buildDesafio(Long.MAX_VALUE, 1));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resposta.getBody()).containsEntry("codigo", "TURMA_NAO_ENCONTRADA");
    }

    @Test
    void atualizar_semPerguntas_deveRetornar400ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> alteracao = buildDesafio(c.turmaId(), 1);
        alteracao.put("perguntas", List.of());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(c.professor(), c.desafioId(), alteracao);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    @Test
    @SuppressWarnings("unchecked")
    void atualizar_comDuasAlternativasCorretas_deveRetornar400EManterAsPerguntasOriginais() {
        // Arrange
        Cenario c = cenario(2);
        Map<String, Object> alteracao = buildDesafio(c.turmaId(), 1);
        ((List<Map<String, Object>>) alteracao.get("perguntas")).get(0).put("alternativas", List.of(
                Map.of("texto", "Certa", "correta", true),
                Map.of("texto", "Também certa", "correta", true)));

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(c.professor(), c.desafioId(), alteracao);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALTERNATIVA_CORRETA_INVALIDA");
        assertThat((List<Object>) buscarParaEdicao(c.professor(), c.desafioId()).getBody().get("perguntas"))
                .hasSize(2);
    }

    // ------------------------------------------------------------------- Listar

    @Test
    void listar_comoAluno_deveTrazerSoDesafiosDaSuaTurma() {
        // Arrange
        Cenario minhaTurma = cenario(1);
        Cenario outraTurma = cenario(1);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, minhaTurma.aluno().token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(IntegracaoBase::id)
                .containsExactly(minhaTurma.desafioId())
                .doesNotContain(outraTurma.desafioId());
    }

    @Test
    void listar_comoProfessor_deveRetornar200ComODesafioCriado() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, c.professor().token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(IntegracaoBase::id).contains(c.desafioId());
    }

    // -------------------------------------------- Início controlado pelo professor

    @Test
    void estado_antesDeIniciar_deveRetornar200SemPerguntaParaOAluno() {
        // Arrange
        Cenario c = cenario(2);

        // Act
        ResponseEntity<Map<String, Object>> resposta = estado(c.aluno(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("status", "AGUARDANDO")
                .containsEntry("totalPerguntas", 2);
        assertThat(resposta.getBody().get("pergunta")).isNull();
    }

    @Test
    void estado_comAlunoForaDaTurma_deveRetornar403ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        Sessao alunoDeFora = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = estado(alunoDeFora, c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_FORA_DA_TURMA");
    }

    @Test
    void iniciar_comoAluno_deveRetornar403ENaoLiberarAPergunta() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = iniciar(c.aluno(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
        assertThat(estado(c.aluno(), c.desafioId()).getBody().get("pergunta")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void iniciar_comoProfessor_deveMostrarAPerguntaComTempoESemGabaritoParaOAluno() {
        // Arrange
        Cenario c = cenario(2);

        // Act
        ResponseEntity<Map<String, Object>> inicio = iniciar(c.professor(), c.desafioId());
        ResponseEntity<Map<String, Object>> visaoDoAluno = estado(c.aluno(), c.desafioId());

        // Assert
        assertThat(inicio.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(visaoDoAluno.getBody()).containsEntry("status", "EM_ANDAMENTO")
                .containsEntry("numeroPergunta", 1)
                .containsEntry("perguntaEncerrada", false);
        assertThat(visaoDoAluno.getBody().get("alternativaCorretaId")).isNull();
        assertThat(((Number) visaoDoAluno.getBody().get("segundosRestantes")).intValue()).isBetween(1, 30);

        Map<String, Object> pergunta = perguntaDe(visaoDoAluno.getBody());
        assertThat(pergunta).containsEntry("enunciado", "Pergunta 1")
                .containsEntry("tempoSegundos", 30);
        assertThat((List<Map<String, Object>>) pergunta.get("alternativas")).hasSize(2)
                .allSatisfy(alternativa -> assertThat(alternativa).doesNotContainKey("correta"));
    }

    @Test
    void iniciar_duasVezes_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        iniciar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = iniciar(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_JA_INICIADO");
    }

    // ------------------------------------------------------------------ Responder

    @Test
    void responder_antesDeIniciar_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenario(1);

        // Act
        ResponseEntity<Map<String, Object>> resposta = responder(c.aluno(), c.desafioId(), 1L, 1L);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DESAFIO_NAO_INICIADO");
    }

    @Test
    void responder_dentroDoTempo_deveRetornar201EGravarSemRevelarOGabarito() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        Long alternativaEscolhida = alternativa(pergunta, 0);

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.desafioId(), id(pergunta), alternativaEscolhida);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).doesNotContainKeys("correta", "pontos");
        assertThat(respostaRepository.findByPergunta_IdAndUsuario_Id(id(pergunta), c.aluno().id())).isPresent();

        Map<String, Object> estado = estado(c.aluno(), c.desafioId()).getBody();
        assertThat(((Number) estado.get("minhaAlternativaId")).longValue()).isEqualTo(alternativaEscolhida);
        assertThat(estado.get("alternativaCorretaId")).isNull();
        assertThat(estado.get("meusPontos")).isNull();
    }

    @Test
    void responder_duasVezes_deveRetornar409EManterAPrimeiraResposta() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 1));

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "PERGUNTA_JA_RESPONDIDA");
        assertThat(respostaRepository.findByPergunta_IdAndUsuario_Id(id(pergunta), c.aluno().id())
                .orElseThrow().getPontos()).isZero();
    }

    @Test
    void responder_depoisDoTempo_deveRetornar409ENaoGravar() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        esgotarTempo(c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "TEMPO_ESGOTADO");
        assertThat(respostaRepository.findByPergunta_IdAndUsuario_Id(id(pergunta), c.aluno().id())).isEmpty();
    }

    @Test
    void responder_perguntaQueNaoEAAtual_deveRetornar400ComCodigo() {
        // Arrange
        Cenario c = cenario(2);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.desafioId(), id(pergunta) + 1, alternativa(pergunta, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "PERGUNTA_NAO_ATIVA");
    }

    @Test
    void responder_comAlternativaDeOutraPergunta_deveRetornar400ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.aluno(), c.desafioId(), id(pergunta), Long.MAX_VALUE);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALTERNATIVA_INVALIDA");
    }

    @Test
    void responder_comoProfessor_deveRetornar403ComCodigo() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(c.professor(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_ALUNO");
    }

    @Test
    void responder_comAlunoForaDaTurma_deveRetornar403ENaoGravar() {
        // Arrange
        Cenario c = cenario(1);
        Sessao alunoDeFora = novaSessao("ALUNO");
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());

        // Act
        ResponseEntity<Map<String, Object>> resposta =
                responder(alunoDeFora, c.desafioId(), id(pergunta), alternativa(pergunta, 0));

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALUNO_FORA_DA_TURMA");
        assertThat(respostaRepository.findByPergunta_IdAndUsuario_Id(id(pergunta), alunoDeFora.id())).isEmpty();
    }

    // -------------------------------------------------------- Avançar e encerrar

    @Test
    void avancar_comPerguntaEmAndamento_deveRetornar409ComCodigo() {
        // Arrange
        Cenario c = cenario(2);
        iniciar(c.professor(), c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = avancar(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "PERGUNTA_EM_ANDAMENTO");
    }

    @Test
    void avancar_comoAluno_deveRetornar403ComCodigo() {
        // Arrange
        Cenario c = cenario(2);
        iniciar(c.professor(), c.desafioId());
        esgotarTempo(c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = avancar(c.aluno(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROFESSOR_OU_ADMINISTRADOR");
    }

    @Test
    void avancar_depoisDoTempo_deveAbrirASegundaPergunta() {
        // Arrange
        Cenario c = cenario(2);
        iniciar(c.professor(), c.desafioId());
        esgotarTempo(c.desafioId());

        // Act
        ResponseEntity<Map<String, Object>> resposta = avancar(c.professor(), c.desafioId());

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("numeroPergunta", 2)
                .containsEntry("perguntaEncerrada", false);
        assertThat(perguntaDe(resposta.getBody())).containsEntry("enunciado", "Pergunta 2");
    }

    @Test
    void fluxoCompleto_doisAlunos_deveMostrarGabaritoPontosERankingEEncerrar() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        Long correta = alternativa(pergunta, 0);

        // Act: um acerta e o outro erra; os dois responderam, então a pergunta encerra antes do tempo
        responder(c.aluno(), c.desafioId(), id(pergunta), correta);
        responder(c.colega(), c.desafioId(), id(pergunta), alternativa(pergunta, 1));

        // Assert: gabarito e pontos liberados
        Map<String, Object> estadoDoAluno = estado(c.aluno(), c.desafioId()).getBody();
        assertThat(estadoDoAluno).containsEntry("perguntaEncerrada", true)
                .containsEntry("segundosRestantes", 0);
        assertThat(((Number) estadoDoAluno.get("alternativaCorretaId")).longValue()).isEqualTo(correta);
        assertThat(((Number) estadoDoAluno.get("meusPontos")).intValue()).isBetween(500, 1000);
        assertThat(estado(c.colega(), c.desafioId()).getBody()).containsEntry("meusPontos", 0);

        // Assert: avançar depois da última pergunta encerra o desafio
        ResponseEntity<Map<String, Object>> fim = avancar(c.professor(), c.desafioId());
        assertThat(fim.getBody()).containsEntry("status", "ENCERRADO");
        assertThat(fim.getBody().get("pergunta")).isNull();
        assertThat(desafioRepository.findById(c.desafioId()).orElseThrow().getStatus())
                .isEqualTo(StatusDesafio.ENCERRADO);

        // Assert: ranking com quem acertou em primeiro
        ResponseEntity<List<Map<String, Object>>> ranking = client.get().uri(BASE_PATH + "/{id}/ranking", c.desafioId())
                .header(HttpHeaders.AUTHORIZATION, c.aluno().token())
                .retrieve()
                .toEntity(LISTA_JSON);
        assertThat(ranking.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ranking.getBody()).hasSize(2);
        assertThat(((Number) ranking.getBody().get(0).get("usuarioId")).longValue()).isEqualTo(c.aluno().id());
        assertThat(ranking.getBody().get(0)).containsEntry("posicao", 1).containsEntry("acertos", 1);
        assertThat(ranking.getBody().get(1)).containsEntry("pontos", 0).containsEntry("acertos", 0);
    }

    @Test
    void ranking_comPerguntaAberta_naoDeveRevelarQuemJaAcertou() {
        // Arrange
        Cenario c = cenario(1);
        Map<String, Object> pergunta = perguntaDe(iniciar(c.professor(), c.desafioId()).getBody());
        responder(c.aluno(), c.desafioId(), id(pergunta), alternativa(pergunta, 0));

        // Act
        ResponseEntity<List<Map<String, Object>>> ranking = client.get().uri(BASE_PATH + "/{id}/ranking", c.desafioId())
                .header(HttpHeaders.AUTHORIZATION, c.colega().token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(ranking.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(ranking.getBody()).isEmpty();
    }

    // ------------------------------------------------------------------- Helpers

    private static Map<String, Object> buildDesafio(Long turmaId, int quantidadePerguntas) {
        List<Map<String, Object>> perguntas = new java.util.ArrayList<>();
        for (int i = 1; i <= quantidadePerguntas; i++) {
            Map<String, Object> pergunta = new HashMap<>();
            pergunta.put("enunciado", "Pergunta " + i);
            pergunta.put("tempoSegundos", 30);
            pergunta.put("alternativas", List.of(
                    Map.of("texto", "Certa " + i, "correta", true),
                    Map.of("texto", "Errada " + i, "correta", false)));
            perguntas.add(pergunta);
        }

        Map<String, Object> desafio = new HashMap<>();
        desafio.put("titulo", nomeUnico("Desafio"));
        desafio.put("descricao", "Desafio de teste em tempo real");
        desafio.put("turmaId", turmaId);
        desafio.put("perguntas", perguntas);
        return desafio;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> perguntaDe(Map<String, Object> estado) {
        return (Map<String, Object>) estado.get("pergunta");
    }

    @SuppressWarnings("unchecked")
    private static Long alternativa(Map<String, Object> pergunta, int posicao) {
        return id(((List<Map<String, Object>>) pergunta.get("alternativas")).get(posicao));
    }

    /** Move o prazo da pergunta para o passado, sem precisar esperar o tempo real. */
    private void esgotarTempo(Long desafioId) {
        Desafio desafio = desafioRepository.findById(desafioId).orElseThrow();
        desafio.setPerguntaExpiraEm(LocalDateTime.now().minusSeconds(1));
        desafioRepository.save(desafio);
    }

    private ResponseEntity<Map<String, Object>> criar(Sessao sessao, Map<String, Object> desafio) {
        return client.post().uri(BASE_PATH)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(desafio)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> buscarParaEdicao(Sessao sessao, Long desafioId) {
        return client.get().uri(BASE_PATH + "/{id}/edicao", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao sessao, Long desafioId, Map<String, Object> desafio) {
        return client.put().uri(BASE_PATH + "/{id}", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(desafio)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> excluir(Sessao sessao, Long desafioId) {
        return client.delete().uri(BASE_PATH + "/{id}", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> iniciar(Sessao sessao, Long desafioId) {
        return client.post().uri(BASE_PATH + "/{id}/iniciar", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> avancar(Sessao sessao, Long desafioId) {
        return client.post().uri(BASE_PATH + "/{id}/avancar", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> estado(Sessao sessao, Long desafioId) {
        return client.get().uri(BASE_PATH + "/{id}/estado", desafioId)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
    }

    @Override
    protected ResponseEntity<Map<String, Object>> responder(Sessao aluno, Long desafioId, Long perguntaId, Long alternativaId) {
        return client.post().uri(BASE_PATH + "/{id}/respostas", desafioId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntaId", perguntaId, "alternativaId", alternativaId))
                .retrieve()
                .toEntity(JSON);
    }
}

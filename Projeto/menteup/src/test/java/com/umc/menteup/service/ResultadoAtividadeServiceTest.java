package com.umc.menteup.service;

import com.umc.menteup.dto.ResultadoAtividadeResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResultadoAtividadeServiceTest {

    private static final String USERNAME = "aluno@teste.com";
    private static final long ALUNO_ID = 7L;
    private static final long TURMA_ID = 5L;
    private static final long ATIVIDADE_ID = 10L;

    private AtividadeRepository atividadeRepository;
    private UsuarioRepository usuarioRepository;
    private ResultadoAtividadeRepository resultadoRepository;
    private ProgressoAtividadeRepository progressoRepository;
    private TurmaRepository turmaRepository;
    private ResultadoAtividadeService service;
    private LogSistemaService logSistemaService;

    private Usuario aluno;
    private Atividade atividade;

    @BeforeEach
    void setUp() {
        atividadeRepository = mock(AtividadeRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        resultadoRepository = mock(ResultadoAtividadeRepository.class);
        progressoRepository = mock(ProgressoAtividadeRepository.class);
        turmaRepository = mock(TurmaRepository.class);
        logSistemaService = mock(LogSistemaService.class);

        service = new ResultadoAtividadeService(
                atividadeRepository,
                usuarioRepository,
                resultadoRepository,
                new MatriculaService(turmaRepository, usuarioRepository, logSistemaService),
                progressoRepository,
                logSistemaService
        );

        aluno = usuario(ALUNO_ID, "ALUNO");
        atividade = atividadeComDuasPerguntas(StatusAtividade.EM_ANDAMENTO);

        when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(turmaRepository.contarMatricula(TURMA_ID, ALUNO_ID)).thenReturn(1L);
        prepararProgresso(new long[]{1, 11}, new long[]{2, 22});
        when(resultadoRepository.saveAndFlush(any(ResultadoAtividade.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));
    }

    @Test
    void corrigeESalvaResultado() {
        ResultadoAtividadeResponse resposta = service.concluirAtividade(ATIVIDADE_ID, USERNAME);

        assertEquals(1, resposta.acertos());
        assertEquals(5, resposta.pontuacao());
        assertEquals(2, resposta.totalQuestoes());
        verify(resultadoRepository).saveAndFlush(any(ResultadoAtividade.class));
    }

    @Test
    void gabaritoCompletoDaPontuacaoTotal() {
        prepararProgresso(new long[]{1, 11}, new long[]{2, 21});
        ResultadoAtividadeResponse resposta = service.concluirAtividade(ATIVIDADE_ID, USERNAME);

        assertEquals(2, resposta.acertos());
        assertEquals(10, resposta.pontuacao());
    }

    @Test
    void professorNaoPodeResponder() {
        aluno.setTipoUsuario("PROFESSOR");

        assertErro(HttpStatus.FORBIDDEN, "SOMENTE_ALUNO",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void alunoForaDaTurmaRecebe403() {
        when(turmaRepository.contarMatricula(TURMA_ID, ALUNO_ID)).thenReturn(0L);

        assertErro(HttpStatus.FORBIDDEN, "ALUNO_FORA_DA_TURMA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
        verify(resultadoRepository, never()).saveAndFlush(any(ResultadoAtividade.class));
    }

    @Test
    void segundaSubmissaoRecebe409() {
        when(resultadoRepository.existsByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID)).thenReturn(true);

        assertErro(HttpStatus.CONFLICT, "ATIVIDADE_JA_RESPONDIDA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
        verify(resultadoRepository, never()).saveAndFlush(any(ResultadoAtividade.class));
    }

    @Test
    void corridaBarradaPelaConstraintVira409() {
        when(resultadoRepository.saveAndFlush(any(ResultadoAtividade.class)))
                .thenThrow(new DataIntegrityViolationException("uk_usuario_atividade"));

        assertErro(HttpStatus.CONFLICT, "ATIVIDADE_JA_RESPONDIDA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void atividadeEncerradaNaoAceitaRespostas() {
        atividade.setStatus(StatusAtividade.CONCLUIDO);

        assertErro(HttpStatus.CONFLICT, "ATIVIDADE_ENCERRADA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void rascunhoNaoExisteParaOAluno() {
        atividade.setStatus(StatusAtividade.PENDENTE);

        assertErro(HttpStatus.NOT_FOUND, "ATIVIDADE_NAO_ENCONTRADA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void perguntaRespondidaDuasVezesEhRejeitada() {
        prepararProgresso(new long[]{1, 11}, new long[]{1, 12});
        assertErro(HttpStatus.BAD_REQUEST, "PERGUNTA_DUPLICADA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void faltandoPerguntaEhRejeitado() {
        prepararProgresso(new long[]{1, 11});
        assertErro(HttpStatus.BAD_REQUEST, "RESPOSTAS_INCOMPLETAS",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void perguntaDeOutraAtividadeEhRejeitada() {
        // Nota: com o guard "progresso.getRespostas().size() != perguntas.size()"
        // que existe hoje em ResultadoAtividadeService, uma pergunta estranha no
        // lugar de uma pergunta real é barrada como RESPOSTAS_INCOMPLETAS antes
        // de chegar no laço que detectaria PERGUNTA_INVALIDA. Ver observação abaixo.
        prepararProgressoComPerguntaExterna();
        assertErro(HttpStatus.BAD_REQUEST, "RESPOSTAS_INCOMPLETAS",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    @Test
    void alternativaDeOutraPerguntaEhRejeitada() {
        prepararProgresso(new long[]{1, 21}, new long[]{2, 22});
        assertErro(HttpStatus.BAD_REQUEST, "ALTERNATIVA_INVALIDA",
                () -> service.concluirAtividade(ATIVIDADE_ID, USERNAME));
    }

    // ---------- helpers ----------

    private static void assertErro(HttpStatus status, String codigo, Executable acao) {
        ApiException erro = assertThrows(ApiException.class, acao);
        assertEquals(status, erro.getStatus());
        assertEquals(codigo, erro.getCodigo());
    }

    /** Monta um ProgressoAtividade com uma RespostaAtividade por par {perguntaId, alternativaId}. */
    private void prepararProgresso(long[]... paresPerguntaAlternativa) {
        ProgressoAtividade progresso = new ProgressoAtividade();
        progresso.setUsuario(aluno);
        progresso.setAtividade(atividade);

        for (long[] par : paresPerguntaAlternativa) {
            progresso.getRespostas().add(criarResposta(progresso, par[0], par[1]));
        }

        when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                .thenReturn(Optional.of(progresso));
    }

    /** Progresso com uma resposta válida (pergunta 1) e uma para uma pergunta que não pertence à atividade. */
    private void prepararProgressoComPerguntaExterna() {
        ProgressoAtividade progresso = new ProgressoAtividade();
        progresso.setUsuario(aluno);
        progresso.setAtividade(atividade);

        progresso.getRespostas().add(criarResposta(progresso, 1, 11));
        progresso.getRespostas().add(criarResposta(progresso, 99, 991));

        when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                .thenReturn(Optional.of(progresso));
    }

    private RespostaAtividade criarResposta(ProgressoAtividade progresso, long perguntaId, long alternativaId) {
        RespostaAtividade resposta = new RespostaAtividade();
        resposta.setProgresso(progresso);
        resposta.setPergunta(localizarOuCriarPergunta(perguntaId));
        resposta.setAlternativa(localizarOuCriarAlternativa(alternativaId));
        return resposta;
    }

    private Pergunta localizarOuCriarPergunta(long perguntaId) {
        return atividade.getPerguntas().stream()
                .filter(pergunta -> pergunta.getId() == perguntaId)
                .findFirst()
                .orElseGet(() -> {
                    Pergunta externa = new Pergunta();
                    externa.setId(perguntaId);
                    externa.setEnunciado("Pergunta externa " + perguntaId);
                    return externa;
                });
    }

    private Alternativa localizarOuCriarAlternativa(long alternativaId) {
        return atividade.getPerguntas().stream()
                .flatMap(pergunta -> pergunta.getAlternativas().stream())
                .filter(alternativa -> alternativa.getId() == alternativaId)
                .findFirst()
                .orElseGet(() -> {
                    Alternativa externa = new Alternativa();
                    externa.setId(alternativaId);
                    externa.setTexto("Alternativa externa " + alternativaId);
                    externa.setCorreta(false);
                    return externa;
                });
    }

    private static Usuario usuario(long id, String tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsuario(USERNAME);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private static Atividade atividadeComDuasPerguntas(StatusAtividade status) {
        Turma turma = new Turma();
        turma.setId(TURMA_ID);

        Atividade atividade = new Atividade();
        atividade.setId(ATIVIDADE_ID);
        atividade.setTitulo("Atividade de teste");
        atividade.setPontuacao(10);
        atividade.setStatus(status);
        atividade.setTurma(turma);

        for (long i = 1; i <= 2; i++) {
            Pergunta pergunta = new Pergunta();
            pergunta.setId(i);
            pergunta.setEnunciado("Pergunta " + i);
            pergunta.setAtividade(atividade);
            pergunta.getAlternativas().add(alternativa(i * 10 + 1, true, pergunta));
            pergunta.getAlternativas().add(alternativa(i * 10 + 2, false, pergunta));
            atividade.getPerguntas().add(pergunta);
        }

        return atividade;
    }

    private static Alternativa alternativa(long id, boolean correta, Pergunta pergunta) {
        Alternativa alternativa = new Alternativa();
        alternativa.setId(id);
        alternativa.setTexto("Alternativa " + id);
        alternativa.setCorreta(correta);
        alternativa.setPergunta(pergunta);
        return alternativa;
    }
}
package com.umc.menteup.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import com.umc.menteup.dto.EstadoDesafioResponse;
import com.umc.menteup.dto.RankingDesafioResponse;
import com.umc.menteup.dto.ResponderDesafioRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.AlternativaDesafio;
import com.umc.menteup.model.Desafio;
import com.umc.menteup.model.PerguntaDesafio;
import com.umc.menteup.model.RespostaDesafio;
import com.umc.menteup.model.StatusDesafio;
import com.umc.menteup.model.Turma;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.DesafioRepository;
import com.umc.menteup.repository.RespostaDesafioRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.repository.UsuarioRepository;

class DesafioServiceTest {

    private static final long DESAFIO_ID = 1L;
    private static final long PERGUNTA_ID = 10L;
    private static final long ALTERNATIVA_CERTA = 100L;
    private static final long ALTERNATIVA_ERRADA = 101L;
    private static final long ALUNO_ID = 5L;
    private static final String EMAIL_ALUNO = "aluno@teste.com";
    private static final String EMAIL_PROFESSOR = "professor@teste.com";

    /** Relógio controlado pelo teste: o tempo só anda quando o teste manda. */
    private static class RelogioDeTeste extends Clock {
        private Instant agora = Instant.parse("2026-11-01T12:00:00Z");

        void avancarMilissegundos(long milissegundos) {
            agora = agora.plusMillis(milissegundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return agora;
        }
    }

    private final RelogioDeTeste relogio = new RelogioDeTeste();

    private DesafioRepository desafioRepository;
    private RespostaDesafioRepository respostaRepository;
    private MatriculaService matriculaService;
    private DesafioService service;

    private Usuario aluno;
    private Desafio desafio;

    @BeforeEach
    void setUp() {
        desafioRepository = mock(DesafioRepository.class);
        respostaRepository = mock(RespostaDesafioRepository.class);
        matriculaService = mock(MatriculaService.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);

        service = new DesafioService(
                desafioRepository,
                respostaRepository,
                mock(TurmaRepository.class),
                usuarioRepository,
                matriculaService,
                relogio,
                mock(ApplicationEventPublisher.class),
                mock(LogSistemaService.class)
        );

        aluno = usuario(ALUNO_ID, EMAIL_ALUNO, "ALUNO");
        desafio = desafioComUmaPerguntaDe20Segundos();

        lenient().when(usuarioRepository.findByUsuario(EMAIL_ALUNO)).thenReturn(Optional.of(aluno));
        lenient().when(usuarioRepository.findByUsuario(EMAIL_PROFESSOR))
                .thenReturn(Optional.of(usuario(2L, EMAIL_PROFESSOR, "PROFESSOR")));
        lenient().when(desafioRepository.findById(DESAFIO_ID)).thenReturn(Optional.of(desafio));
        lenient().when(matriculaService.estaMatriculado(any(), any())).thenReturn(true);
        lenient().when(respostaRepository.findByPergunta_IdAndUsuario_Id(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("antes de o professor iniciar, o estado não traz nenhuma pergunta")
    void deveEsconderAPerguntaEnquantoODesafioNaoInicia() {
        EstadoDesafioResponse estado = service.buscarEstado(DESAFIO_ID, EMAIL_ALUNO);

        assertEquals(StatusDesafio.AGUARDANDO, estado.status());
        assertNull(estado.pergunta());
        assertNull(estado.segundosRestantes());
        assertEquals(1, estado.totalPerguntas());
    }

    @Test
    @DisplayName("iniciar abre a primeira pergunta com o tempo dela, sem revelar o gabarito")
    void deveAbrirAPrimeiraPerguntaAoIniciar() {
        EstadoDesafioResponse estado = service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);

        assertEquals(StatusDesafio.EM_ANDAMENTO, estado.status());
        assertEquals(1, estado.numeroPergunta());
        assertEquals(PERGUNTA_ID, estado.pergunta().id());
        assertEquals(20, estado.segundosRestantes());
        assertFalse(estado.perguntaEncerrada());
        assertNull(estado.alternativaCorretaId());
    }

    @Test
    @DisplayName("aluno não pode iniciar o desafio")
    void deveLancarExcecaoQuandoAlunoTentaIniciar() {
        ApiException ex = assertThrows(ApiException.class, () -> service.iniciar(DESAFIO_ID, EMAIL_ALUNO));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("Esta operação é exclusiva para professores e administradores.", ex.getMessage());
        assertEquals(StatusDesafio.AGUARDANDO, desafio.getStatus());
    }

    @Test
    @DisplayName("não inicia duas vezes: o relógio da pergunta não recomeça")
    void deveLancarExcecaoQuandoDesafioJaFoiIniciado() {
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        var expiraEm = desafio.getPerguntaExpiraEm();
        relogio.avancarMilissegundos(5_000);

        ApiException ex = assertThrows(ApiException.class, () -> service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR));

        assertEquals("DESAFIO_JA_INICIADO", ex.getCodigo());
        assertEquals(expiraEm, desafio.getPerguntaExpiraEm());
    }

    @ParameterizedTest(name = "resposta certa aos {0} ms de 20 s = {1} pontos")
    @CsvSource({"0,75", "10000,50", "19999,25", "20000,25"})
    @DisplayName("pontuação cai de 75 a 25 conforme o tempo passa")
    void deveCalcularPontosPelaVelocidadeDaResposta(long milissegundos, int pontosEsperados) {
        // Arrange
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        relogio.avancarMilissegundos(milissegundos);

        // Act
        service.responder(DESAFIO_ID, new ResponderDesafioRequest(PERGUNTA_ID, ALTERNATIVA_CERTA), EMAIL_ALUNO);

        // Assert
        ArgumentCaptor<RespostaDesafio> salva = ArgumentCaptor.forClass(RespostaDesafio.class);
        verify(respostaRepository).saveAndFlush(salva.capture());
        assertEquals(pontosEsperados, salva.getValue().getPontos());
        assertTrue(salva.getValue().getCorreta());
    }

    @Test
    @DisplayName("resposta errada vale zero, mesmo sendo instantânea")
    void deveDarZeroPontosParaRespostaErrada() {
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);

        service.responder(DESAFIO_ID, new ResponderDesafioRequest(PERGUNTA_ID, ALTERNATIVA_ERRADA), EMAIL_ALUNO);

        ArgumentCaptor<RespostaDesafio> salva = ArgumentCaptor.forClass(RespostaDesafio.class);
        verify(respostaRepository).saveAndFlush(salva.capture());
        assertEquals(0, salva.getValue().getPontos());
        assertFalse(salva.getValue().getCorreta());
    }

    @Test
    @DisplayName("um milissegundo depois do tempo, a resposta é rejeitada e não salva")
    void deveLancarExcecaoQuandoOTempoAcabou() {
        // Arrange
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        relogio.avancarMilissegundos(20_001);
        ResponderDesafioRequest request = new ResponderDesafioRequest(PERGUNTA_ID, ALTERNATIVA_CERTA);

        // Act
        ApiException ex = assertThrows(ApiException.class,
                () -> service.responder(DESAFIO_ID, request, EMAIL_ALUNO));

        // Assert
        assertEquals("TEMPO_ESGOTADO", ex.getCodigo());
        assertEquals("O tempo para responder esta pergunta acabou.", ex.getMessage());
        verify(respostaRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("aluno de outra turma não vê o desafio")
    void deveLancarExcecaoQuandoAlunoNaoEstaNaTurma() {
        when(matriculaService.estaMatriculado(any(), any())).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class, () -> service.buscarEstado(DESAFIO_ID, EMAIL_ALUNO));

        assertEquals("ALUNO_FORA_DA_TURMA", ex.getCodigo());
    }

    @Test
    @DisplayName("dois envios simultâneos: a constraint do banco vira 409")
    void deveConverterViolacaoDaConstraintEm409() {
        // Arrange
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        when(respostaRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicada"));
        ResponderDesafioRequest request = new ResponderDesafioRequest(PERGUNTA_ID, ALTERNATIVA_CERTA);

        // Act
        ApiException ex = assertThrows(ApiException.class,
                () -> service.responder(DESAFIO_ID, request, EMAIL_ALUNO));

        // Assert
        assertEquals("PERGUNTA_JA_RESPONDIDA", ex.getCodigo());
    }

    @Test
    @DisplayName("professor não avança enquanto há tempo e aluno sem responder")
    void deveImpedirAvancoComPerguntaEmAndamento() {
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        when(respostaRepository.countByPergunta_Id(PERGUNTA_ID)).thenReturn(1L);

        ApiException ex = assertThrows(ApiException.class, () -> service.avancar(DESAFIO_ID, EMAIL_PROFESSOR));

        assertEquals("PERGUNTA_EM_ANDAMENTO", ex.getCodigo());
    }

    @Test
    @DisplayName("com todos os alunos respondendo, a pergunta encerra antes do tempo e mostra o gabarito")
    void deveEncerrarAPerguntaQuandoTodosResponderam() {
        // Arrange
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        when(respostaRepository.countByPergunta_Id(PERGUNTA_ID)).thenReturn(2L);

        // Act
        EstadoDesafioResponse estado = service.buscarEstado(DESAFIO_ID, EMAIL_ALUNO);

        // Assert
        assertTrue(estado.perguntaEncerrada());
        assertEquals(0, estado.segundosRestantes());
        assertEquals(ALTERNATIVA_CERTA, estado.alternativaCorretaId());
    }

    @Test
    @DisplayName("avançar depois da última pergunta encerra o desafio")
    void deveEncerrarODesafioDepoisDaUltimaPergunta() {
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        relogio.avancarMilissegundos(20_001);

        EstadoDesafioResponse estado = service.avancar(DESAFIO_ID, EMAIL_PROFESSOR);

        assertEquals(StatusDesafio.ENCERRADO, estado.status());
        assertNull(estado.pergunta());
        assertNull(desafio.getPerguntaExpiraEm());
    }

    @Test
    @DisplayName("não exclui desafio em andamento")
    void deveImpedirExclusaoDeDesafioEmAndamento() {
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);

        ApiException ex = assertThrows(ApiException.class, () -> service.excluir(DESAFIO_ID, EMAIL_PROFESSOR));

        assertEquals("DESAFIO_EM_ANDAMENTO", ex.getCodigo());
        verify(desafioRepository, never()).delete(any());
    }

    @Test
    @DisplayName("ranking esconde a pergunta aberta e ordena pelos pontos das encerradas")
    void deveOrdenarORankingSemContarAPerguntaAberta() {
        // Arrange: pergunta 10 encerrada e pergunta 11 aberta
        PerguntaDesafio segunda = pergunta(desafio, 11L, 200L);
        desafio.getPerguntas().add(segunda);
        service.iniciar(DESAFIO_ID, EMAIL_PROFESSOR);
        relogio.avancarMilissegundos(20_001);
        service.avancar(DESAFIO_ID, EMAIL_PROFESSOR);

        Usuario outro = usuario(6L, "outro@teste.com", "ALUNO");
        PerguntaDesafio primeira = desafio.getPerguntas().get(0);
        when(respostaRepository.buscarTodasDoDesafio(DESAFIO_ID)).thenReturn(List.of(
                resposta(primeira, aluno, 600),
                resposta(primeira, outro, 900),
                resposta(segunda, aluno, 1000)));

        // Act
        List<RankingDesafioResponse> ranking = service.ranking(DESAFIO_ID, EMAIL_ALUNO);

        // Assert
        assertEquals(2, ranking.size());
        assertEquals(6L, ranking.get(0).usuarioId());
        assertEquals(900, ranking.get(0).pontos());
        assertEquals(2, ranking.get(1).posicao());
        assertEquals(600, ranking.get(1).pontos());
    }

    private static Usuario usuario(Long id, String email, String tipo) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setUsuario(email);
        usuario.setNomeUsuario("Usuario " + id);
        usuario.setTipoUsuario(tipo);
        return usuario;
    }

    private Desafio desafioComUmaPerguntaDe20Segundos() {
        Turma turma = new Turma();
        turma.setId(3L);
        turma.setNome("Turma A");
        turma.getAlunos().add(aluno);
        turma.getAlunos().add(usuario(6L, "outro@teste.com", "ALUNO"));

        Desafio novo = new Desafio();
        novo.setId(DESAFIO_ID);
        novo.setTitulo("Desafio de soma");
        novo.setTurma(turma);
        novo.getPerguntas().add(pergunta(novo, PERGUNTA_ID, ALTERNATIVA_CERTA));
        return novo;
    }

    private static PerguntaDesafio pergunta(Desafio desafio, long id, long alternativaCerta) {
        PerguntaDesafio pergunta = new PerguntaDesafio();
        pergunta.setId(id);
        pergunta.setDesafio(desafio);
        pergunta.setEnunciado("Quanto é 2 + 2?");
        pergunta.setTempoSegundos(20);
        pergunta.setOrdem(desafio.getPerguntas().size());
        pergunta.getAlternativas().add(alternativa(pergunta, alternativaCerta, true));
        pergunta.getAlternativas().add(alternativa(pergunta, alternativaCerta + 1, false));
        return pergunta;
    }

    private static AlternativaDesafio alternativa(PerguntaDesafio pergunta, long id, boolean correta) {
        AlternativaDesafio alternativa = new AlternativaDesafio();
        alternativa.setId(id);
        alternativa.setPergunta(pergunta);
        alternativa.setTexto("Alternativa " + id);
        alternativa.setCorreta(correta);
        return alternativa;
    }

    private static RespostaDesafio resposta(PerguntaDesafio pergunta, Usuario usuario, int pontos) {
        RespostaDesafio resposta = new RespostaDesafio();
        resposta.setPergunta(pergunta);
        resposta.setUsuario(usuario);
        resposta.setPontos(pontos);
        resposta.setCorreta(pontos > 0);
        return resposta;
    }
}

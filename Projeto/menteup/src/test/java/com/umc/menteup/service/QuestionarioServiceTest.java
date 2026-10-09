package com.umc.menteup.service;

import com.umc.menteup.dto.AlternativaRequest;
import com.umc.menteup.dto.PerguntaRequest;
import com.umc.menteup.dto.QuestionarioRequest;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionarioServiceTest {

    private static final String USERNAME = "prof@teste.com";
    private static final long ATIVIDADE_ID = 10L;

    @Mock private AtividadeRepository atividadeRepository;
    @Mock private LogSistemaService logSistemaService;

    @InjectMocks
    private QuestionarioService service;

    @Test
    @DisplayName("atividade inexistente: lança erro, não salva e não registra log")
    void atividadeInexistente() {
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.empty());

        // RuntimeException cobre o comportamento atual e também uma futura ApiException 404.
        assertThrows(RuntimeException.class,
                () -> service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME));

        verify(atividadeRepository, never()).save(any());
        verifyNoInteractions(logSistemaService);
    }

    @Test
    @DisplayName("cria as perguntas e alternativas com os vínculos pai/filho corretos")
    void criaPerguntasEAlternativasVinculadas() {
        Atividade atividade = atividadeVazia();
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> simularIdentity(inv.getArgument(0)));

        Atividade salva = service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME);

        assertSame(atividade, salva);
        assertEquals(2, salva.getPerguntas().size());

        Pergunta p1 = salva.getPerguntas().get(0);
        assertEquals("Quanto e 1 + 1?", p1.getEnunciado());
        assertSame(atividade, p1.getAtividade(), "a pergunta deve apontar para a atividade");
        assertEquals(3, p1.getAlternativas().size());
        assertEquals("1", p1.getAlternativas().get(0).getTexto());
        assertFalse(p1.getAlternativas().get(0).getCorreta());
        assertEquals("2", p1.getAlternativas().get(1).getTexto());
        assertTrue(p1.getAlternativas().get(1).getCorreta());
        for (Alternativa alternativa : p1.getAlternativas()) {
            assertSame(p1, alternativa.getPergunta(), "a alternativa deve apontar para a pergunta");
        }

        Pergunta p2 = salva.getPerguntas().get(1);
        assertEquals("Quanto e 2 + 2?", p2.getEnunciado());
        assertEquals(2, p2.getAlternativas().size());
        assertSame(p2, p2.getAlternativas().get(0).getPergunta());
    }

    @Test
    @DisplayName("mantém as perguntas que já existiam e acrescenta as novas no fim")
    void acrescentaSemApagarAsExistentes() {
        Atividade atividade = atividadeVazia();
        Pergunta antiga = new Pergunta();
        antiga.setId(1L);
        antiga.setEnunciado("Pergunta antiga");
        antiga.setAtividade(atividade);
        atividade.getPerguntas().add(antiga);

        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> simularIdentity(inv.getArgument(0)));

        Atividade salva = service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME);

        assertEquals(3, salva.getPerguntas().size());
        assertSame(antiga, salva.getPerguntas().get(0));
        assertEquals("Quanto e 1 + 1?", salva.getPerguntas().get(1).getEnunciado());
        assertEquals("Quanto e 2 + 2?", salva.getPerguntas().get(2).getEnunciado());
    }

    @Test
    @DisplayName("registra um log CRIAR_QUESTAO por pergunta criada, já com o id gerado pelo banco")
    void umLogPorPerguntaCriada() {
        Atividade atividade = atividadeVazia();
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> simularIdentity(inv.getArgument(0)));

        service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME);

        verify(logSistemaService).registrarPorUsername(
                eq(USERNAME), eq(AcaoLog.CRIAR_QUESTAO), eq(EntidadeLog.QUESTAO),
                eq(101L), eq(ResultadoLog.SUCESSO), anyString(), any());
        verify(logSistemaService).registrarPorUsername(
                eq(USERNAME), eq(AcaoLog.CRIAR_QUESTAO), eq(EntidadeLog.QUESTAO),
                eq(102L), eq(ResultadoLog.SUCESSO), anyString(), any());
        verify(logSistemaService, times(2)).registrarPorUsername(
                any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("o campo 'dados' do log leva o id da atividade e o enunciado da pergunta")
    void dadosDoLog() {
        Atividade atividade = atividadeVazia();
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> simularIdentity(inv.getArgument(0)));

        service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME);

        ArgumentCaptor<Object> dados = ArgumentCaptor.forClass(Object.class);
        verify(logSistemaService, times(2)).registrarPorUsername(
                any(), any(), any(), any(), any(), anyString(), dados.capture());

        Map<?, ?> primeiro = (Map<?, ?>) dados.getAllValues().get(0);
        assertEquals(ATIVIDADE_ID, primeiro.get("atividadeId"));
        assertEquals("Quanto e 1 + 1?", primeiro.get("enunciado"));

        Map<?, ?> segundo = (Map<?, ?>) dados.getAllValues().get(1);
        assertEquals("Quanto e 2 + 2?", segundo.get("enunciado"));
    }

    @Test
    @DisplayName("se o save falhar, nenhum log de sucesso é registrado")
    void saveFalhaNaoGeraLog() {
        Atividade atividade = atividadeVazia();
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(atividadeRepository.save(any(Atividade.class))).thenThrow(new RuntimeException("banco fora do ar"));

        assertThrows(RuntimeException.class,
                () -> service.cadastrarQuesitonario(ATIVIDADE_ID, requestComDuasPerguntas(), USERNAME));

        verifyNoInteractions(logSistemaService);
    }

    private static Atividade atividadeVazia() {
        Atividade a = new Atividade();
        a.setId(ATIVIDADE_ID);
        a.setTitulo("Atividade de soma");
        a.setDescricao("Descricao da atividade de soma");
        a.setPontuacao(10);
        a.setStatus(StatusAtividade.EM_ANDAMENTO);
        return a;
    }

    private static QuestionarioRequest requestComDuasPerguntas() {
        return new QuestionarioRequest(List.of(
                new PerguntaRequest("Quanto e 1 + 1?", List.of(
                        new AlternativaRequest("1", false),
                        new AlternativaRequest("2", true),
                        new AlternativaRequest("3", false)
                )),
                new PerguntaRequest("Quanto e 2 + 2?", List.of(
                        new AlternativaRequest("4", true),
                        new AlternativaRequest("5", false)
                ))
        ));
    }

    private static Atividade simularIdentity(Atividade atividade) {
        long proximo = 101L;
        for (Pergunta pergunta : atividade.getPerguntas()) {
            if (pergunta.getId() == null) {
                pergunta.setId(proximo++);
            }
        }
        return atividade;
    }
}

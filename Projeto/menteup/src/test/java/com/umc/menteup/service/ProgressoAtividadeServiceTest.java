package com.umc.menteup.service;

import com.umc.menteup.dto.ProgressoAtividadeResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.ProgressoAtividadeRepository;
import com.umc.menteup.repository.ResultadoAtividadeRepository;
import com.umc.menteup.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProgressoAtividadeServiceTest {

    private static final String USERNAME = "aluno@teste.com";
    private static final long ALUNO_ID = 7L;
    private static final long ATIVIDADE_ID = 10L;

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AtividadeRepository atividadeRepository;
    @Mock
    private ProgressoAtividadeRepository progressoRepository;
    @Mock
    private ResultadoAtividadeRepository resultadoRepository;
    @Mock
    private LogSistemaService logSistemaService;

    @InjectMocks
    private ProgressoAtividadeService service;

    @Nested
    @DisplayName("validações")
    class Validacoes {

        @Test
        @DisplayName("401 USUARIO_NAO_ENCONTRADO quando o usuário do token não existe")
        void usuarioInexistente() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10011L)));

            assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
            assertEquals("USUARIO_NAO_ENCONTRADO", ex.getCodigo());
            verifyNoInteractions(atividadeRepository, progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("403 SOMENTE_ALUNO quando quem responder for o professor")
        void professorNaoResponde() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(usuario(1L, "PROFESSOR")));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10011L)));

            assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
            assertEquals("SOMENTE_ALUNO", ex.getCodigo());
            verifyNoInteractions(atividadeRepository, progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("404 ATIVIDADE_NAO_ENCONTRADA se a atividade não existir")
        void atividadeInexistente() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno()));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.empty());

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10011L)));

            assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
            assertEquals("ATIVIDADE_NAO_ENCONTRADA", ex.getCodigo());
            verifyNoInteractions(progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("409 ATIVIDADE_JA_RESPONDIDA se o aluno já tiver concluido a atividade")
        void atividadeJaConcluida() {
            when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno()));
            when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade(2)));
            when(resultadoRepository.existsByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID)).thenReturn(true);

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10011L)));

            assertEquals(HttpStatus.CONFLICT, ex.getStatus());
            assertEquals("ATIVIDADE_JA_RESPONDIDA", ex.getCodigo());
            verifyNoInteractions(progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("400 PERGUNTA_INVALIDA se a pergunta é for de uma outra atividade")
        void perguntaDeOutraAtividade() {
            prepararAlunoEAtividade(atividade(2));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(9999L, 10011L)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("PERGUNTA_INVALIDA", ex.getCodigo());
            verifyNoInteractions(progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("400 ALTERNATIVA_INVALIDA se a alternativa for de uma outra pergunta")
        void alternativaDeOutraPergunta() {
            prepararAlunoEAtividade(atividade(2));

            // 10021 é alternativa da pergunta 1002, não da 1001
            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10021L)));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
            assertEquals("ALTERNATIVA_INVALIDA", ex.getCodigo());
            verifyNoInteractions(progressoRepository, logSistemaService);
        }

        @Test
        @DisplayName("400 PERGUNTA_INVALIDA se a atividade não tiver pergunta")
        void atividadeSemPerguntas() {
            prepararAlunoEAtividade(atividade(0));

            ApiException ex = assertThrows(ApiException.class,
                    () -> service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10011L)));

            assertEquals("PERGUNTA_INVALIDA", ex.getCodigo());
            verifyNoInteractions(progressoRepository, logSistemaService);
        }
    }

    @Nested
    @DisplayName("primeira resposta eviada da atividade")
    class PrimeiraResposta {

        @Test
        @DisplayName("cria o progresso do aluno com a resposta e salva")
        void criaProgressoComAResposta() {
            Atividade atividade = atividade(2);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());

            service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

            ArgumentCaptor<ProgressoAtividade> captor = ArgumentCaptor.forClass(ProgressoAtividade.class);
            verify(progressoRepository).save(captor.capture());

            ProgressoAtividade salvo = captor.getValue();
            assertSame(aluno, salvo.getUsuario());
            assertSame(atividade, salvo.getAtividade());
            assertEquals(1, salvo.getRespostas().size());

            RespostaAtividade resposta = salvo.getRespostas().get(0);
            assertSame(salvo, resposta.getProgresso(), "a resposta deve apontar para o progresso");
            assertEquals(1001L, resposta.getPergunta().getId());
            assertEquals(10012L, resposta.getAlternativa().getId());
        }

        @Test
        @DisplayName("devolve o progresso calculado: 1 de 2 perguntas = 50%")
        void devolveProgresso() {
            prepararAlunoEAtividade(atividade(2));
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());

            ProgressoAtividadeResponse resposta =
                    service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

            assertEquals(50, resposta.percentualConclusao());
            assertEquals(1, resposta.respondidas());
            assertEquals(2, resposta.totalQuestoes());
            assertEquals(1, resposta.respostas().size());
            assertEquals(1001L, resposta.respostas().get(0).perguntaId());
            assertEquals(10012L, resposta.respostas().get(0).alternativaId());
        }

        @Test
        @DisplayName("registra INICIAR_ATIVIDADE e depois RESPONDER_QUESTAO, nessa ordem")
        void registraIniciarEResponder() {
            Usuario aluno = prepararAlunoEAtividade(atividade(2));
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.empty());

            service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

            InOrder ordem = inOrder(logSistemaService);
            ordem.verify(logSistemaService).registrarSucesso(
                    eq(aluno), eq(AcaoLog.INICIAR_ATIVIDADE), eq(EntidadeLog.ATIVIDADE),
                    eq(ATIVIDADE_ID), anyString(), any());
            ordem.verify(logSistemaService).registrarSucesso(
                    eq(aluno), eq(AcaoLog.RESPONDER_QUESTAO), eq(EntidadeLog.QUESTAO),
                    eq(1001L), anyString(), any());
        }
    }

    @Nested
    @DisplayName("respostas seguintes (progresso já existe)")
    class RespostasSeguintes {

        @Test
        @DisplayName("responder outra pergunta acrescenta no mesmo progresso (2 de 2 = 100%)")
        void acrescentaNoMesmoProgresso() {
            Atividade atividade = atividade(2);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            ProgressoAtividade existente = progressoComResposta(aluno, atividade, 0, 0); // pergunta 1 -> alt 1
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(existente));

            ProgressoAtividadeResponse resposta =
                    service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1002L, 10021L));

            ArgumentCaptor<ProgressoAtividade> captor = ArgumentCaptor.forClass(ProgressoAtividade.class);
            verify(progressoRepository).save(captor.capture());
            assertSame(existente, captor.getValue(), "não pode criar um segundo progresso");
            assertEquals(2, existente.getRespostas().size());

            assertEquals(100, resposta.percentualConclusao());
            assertEquals(2, resposta.respondidas());
        }

        @Test
        @DisplayName("não registra INICIAR_ATIVIDADE de novo, só RESPONDER_QUESTAO")
        void naoReiniciaAtividade() {
            Atividade atividade = atividade(2);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(progressoComResposta(aluno, atividade, 0, 0)));

            service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1002L, 10021L));

            verify(logSistemaService, never()).registrarSucesso(
                    any(), eq(AcaoLog.INICIAR_ATIVIDADE), any(), any(), any(), any());
            verify(logSistemaService).registrarSucesso(
                    eq(aluno), eq(AcaoLog.RESPONDER_QUESTAO), eq(EntidadeLog.QUESTAO),
                    eq(1002L), anyString(), any());
        }

        @Test
        @DisplayName("responder a mesma pergunta de novo troca a alternativa, sem duplicar a resposta")
        void trocaAAlternativaSemDuplicar() {
            Atividade atividade = atividade(2);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            ProgressoAtividade existente = progressoComResposta(aluno, atividade, 0, 0); // pergunta 1 -> alt 1
            RespostaAtividade respostaOriginal = existente.getRespostas().get(0);
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(existente));

            ProgressoAtividadeResponse resposta =
                    service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

            assertEquals(1, existente.getRespostas().size(), "continua com uma única resposta");
            assertSame(respostaOriginal, existente.getRespostas().get(0), "reaproveita a resposta existente");
            assertEquals(10012L, existente.getRespostas().get(0).getAlternativa().getId());

            assertEquals(50, resposta.percentualConclusao());
            assertEquals(1, resposta.respondidas());
            assertEquals(10012L, resposta.respostas().get(0).alternativaId());
        }

        @Test
        @DisplayName("trocar a alternativa também registra RESPONDER_QUESTAO (e não INICIAR)")
        void trocarAlternativaRegistraLog() {
            Atividade atividade = atividade(2);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(progressoComResposta(aluno, atividade, 0, 0)));

            service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

            verify(logSistemaService, times(1)).registrarSucesso(
                    any(), eq(AcaoLog.RESPONDER_QUESTAO), any(), any(), any(), any());
            verify(logSistemaService, never()).registrarSucesso(
                    any(), eq(AcaoLog.INICIAR_ATIVIDADE), any(), any(), any(), any());
        }

        @Test
        @DisplayName("percentual é arredondado: 2 de 3 perguntas = 67%")
        void percentualArredondado() {
            Atividade atividade = atividade(3);
            Usuario aluno = prepararAlunoEAtividade(atividade);
            when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                    .thenReturn(Optional.of(progressoComResposta(aluno, atividade, 0, 0)));

            ProgressoAtividadeResponse resposta =
                    service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1002L, 10021L));

            assertEquals(67, resposta.percentualConclusao());
            assertEquals(2, resposta.respondidas());
            assertEquals(3, resposta.totalQuestoes());
        }
    }

    @Test
    @DisplayName("o 'dados' do log RESPONDER_QUESTAO leva atividadeId, perguntaId e alternativaId")
    void dadosDoLogDeResposta() {
        prepararAlunoEAtividade(atividade(2));
        when(progressoRepository.findByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID))
                .thenReturn(Optional.empty());

        service.salvarResposta(ATIVIDADE_ID, USERNAME, new SalvarRespostaRequest(1001L, 10012L));

        ArgumentCaptor<Object> dados = ArgumentCaptor.forClass(Object.class);
        verify(logSistemaService).registrarSucesso(
                any(), eq(AcaoLog.RESPONDER_QUESTAO), any(), any(), anyString(), dados.capture());

        Map<?, ?> mapa = (Map<?, ?>) dados.getValue();
        assertEquals(ATIVIDADE_ID, mapa.get("atividadeId"));
        assertEquals(1001L, mapa.get("perguntaId"));
        assertEquals(10012L, mapa.get("alternativaId"));
    }


    private Usuario prepararAlunoEAtividade(Atividade atividade) {
        Usuario aluno = aluno();
        when(usuarioRepository.findByUsuario(USERNAME)).thenReturn(Optional.of(aluno));
        when(atividadeRepository.findById(ATIVIDADE_ID)).thenReturn(Optional.of(atividade));
        when(resultadoRepository.existsByUsuario_IdAndAtividade_Id(ALUNO_ID, ATIVIDADE_ID)).thenReturn(false);
        return aluno;
    }

    private static Usuario aluno() {
        return usuario(ALUNO_ID, "ALUNO");
    }

    private static Usuario usuario(long id, String tipo) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setTipoUsuario(tipo);
        u.setNomeUsuario("Usuario " + id);
        u.setUsuario("usuario" + id + "@teste.com");
        return u;
    }


    private static Atividade atividade(int qtdPerguntas) {
        Atividade a = new Atividade();
        a.setId(ATIVIDADE_ID);
        a.setTitulo("Atividade " + ATIVIDADE_ID);
        a.setDescricao("Descricao da atividade " + ATIVIDADE_ID);
        a.setPontuacao(10);
        a.setStatus(StatusAtividade.EM_ANDAMENTO);

        for (int i = 1; i <= qtdPerguntas; i++) {
            Pergunta p = new Pergunta();
            p.setId(ATIVIDADE_ID * 100 + i);
            p.setEnunciado("Pergunta " + i);
            p.setAtividade(a);

            for (int j = 1; j <= 2; j++) {
                Alternativa alt = new Alternativa();
                alt.setId(p.getId() * 10 + j);
                alt.setTexto("Alternativa " + j);
                alt.setCorreta(j == 1);
                alt.setPergunta(p);
                p.getAlternativas().add(alt);
            }
            a.getPerguntas().add(p);
        }
        return a;
    }

    private static ProgressoAtividade progressoComResposta(Usuario aluno, Atividade atividade, int p, int alt) {
        ProgressoAtividade progresso = new ProgressoAtividade();
        progresso.setUsuario(aluno);
        progresso.setAtividade(atividade);

        Pergunta pergunta = atividade.getPerguntas().get(p);
        RespostaAtividade resposta = new RespostaAtividade();
        resposta.setProgresso(progresso);
        resposta.setPergunta(pergunta);
        resposta.setAlternativa(pergunta.getAlternativas().get(alt));
        progresso.getRespostas().add(resposta);
        return progresso;
    }
}

package com.umc.menteup.controller;

import com.umc.menteup.dto.AtividadeResponse;
import com.umc.menteup.dto.ProgressoAtividadeResponse;
import com.umc.menteup.dto.QuestionarioResponse;
import com.umc.menteup.dto.RespostaProgressoResponse;
import com.umc.menteup.dto.SalvarRespostaRequest;
import com.umc.menteup.dto.TurmaResponse;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.exception.GlobalExceptionHandler;
import com.umc.menteup.model.*;
import com.umc.menteup.repository.AtividadeRepository;
import com.umc.menteup.repository.TurmaRepository;
import com.umc.menteup.service.AtividadeService;
import com.umc.menteup.service.LogSistemaService;
import com.umc.menteup.service.ProgressoAtividadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class AtividadeControllerTest {

    private static final String USERNAME = "prof@teste.com";

    @Mock private AtividadeRepository atividadeRepository;
    @Mock private TurmaRepository turmaRepository;
    @Mock private AtividadeService atividadeService;
    @Mock private ProgressoAtividadeService progressoAtividadeService;
    @Mock private LogSistemaService logSistemaService;

    @InjectMocks
    private AtividadeController controller;

    private MockMvc mockMvc;
    private Authentication autenticado;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(logSistemaService))
                .build();
        autenticado = new UsernamePasswordAuthenticationToken(USERNAME, null, List.of());
    }

    // GET
    @Nested
    @DisplayName("GET /atividades")
    class Listagem {

        @Test
        @DisplayName("lista todas as atividades")
        void listaTodas() throws Exception {
            when(atividadeRepository.findAll()).thenReturn(List.of(
                    atividadeSalva(1L, "Fracoes basicas", StatusAtividade.EM_ANDAMENTO),
                    atividadeSalva(2L, "Geometria plana", StatusAtividade.PENDENTE)
            ));

            mockMvc.perform(get("/atividades").principal(autenticado))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].titulo").value("Fracoes basicas"))
                    .andExpect(jsonPath("$[1].status").value("PENDENTE"));
        }

        @Test
        @DisplayName("busca por título delega ao repository (ignorando maiúsculas)")
        void buscaPorTitulo() throws Exception {
            when(atividadeRepository.findAllByTituloContainingIgnoreCase("frac")).thenReturn(List.of(
                    atividadeSalva(1L, "Fracoes basicas", StatusAtividade.EM_ANDAMENTO)
            ));

            mockMvc.perform(get("/atividades/titulo/frac").principal(autenticado))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].id").value(1));
        }

        @Test
        @DisplayName("busca por título sem resultado devolve lista vazia (200)")
        void buscaPorTituloSemResultado() throws Exception {
            when(atividadeRepository.findAllByTituloContainingIgnoreCase("xyz")).thenReturn(List.of());

            mockMvc.perform(get("/atividades/titulo/xyz").principal(autenticado))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    @Nested
    @DisplayName("GET /atividades/{id}")
    class BuscarPorId {

        @Test
        @DisplayName("200 com o questionário montado pelo service (usa o username do token)")
        void retornaQuestionario() throws Exception {
            QuestionarioResponse resposta = new QuestionarioResponse(
                    new AtividadeResponse(10L, "Fracoes basicas", "Atividade sobre fracoes",
                            new TurmaResponse(5L, "Turma A"), 10, StatusAtividade.EM_ANDAMENTO, null, null),
                    List.of(),
                    null,
                    null
            );
            when(atividadeService.buscarQuestionario(10L, USERNAME)).thenReturn(resposta);

            mockMvc.perform(get("/atividades/10").principal(autenticado))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.atividade.id").value(10))
                    .andExpect(jsonPath("$.atividade.status").value("EM_ANDAMENTO"))
                    .andExpect(jsonPath("$.atividade.turma.id").value(5))
                    .andExpect(jsonPath("$.perguntas.length()").value(0))
                    .andExpect(jsonPath("$.meuResultado").doesNotExist());

            verify(atividadeService).buscarQuestionario(10L, USERNAME);
        }

        @Test
        @DisplayName("404 com codigo ATIVIDADE_NAO_ENCONTRADA quando o service não acha a atividade")
        void naoEncontrada() throws Exception {
            when(atividadeService.buscarQuestionario(99L, USERNAME))
                    .thenThrow(ApiException.naoEncontrado("ATIVIDADE_NAO_ENCONTRADA", "Atividade nao encontrada."));

            mockMvc.perform(get("/atividades/99").principal(autenticado))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("ATIVIDADE_NAO_ENCONTRADA"));
        }

        @Test
        @DisplayName("403 com codigo ALUNO_FORA_DA_TURMA quando o aluno não pertence à turma")
        void alunoForaDaTurma() throws Exception {
            when(atividadeService.buscarQuestionario(10L, USERNAME))
                    .thenThrow(ApiException.proibido("ALUNO_FORA_DA_TURMA", "Voce nao pertence a turma desta atividade."));

            mockMvc.perform(get("/atividades/10").principal(autenticado))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.codigo").value("ALUNO_FORA_DA_TURMA"));
        }
    }

    // POST
    @Nested
    @DisplayName("POST /atividades")
    class Criar {

        @Test
        @DisplayName("201: gera id novo, salva e registra o log CRIAR_ATIVIDADE")
        void criaERegistraLog() throws Exception {
            when(turmaRepository.existsById(1L)).thenReturn(true);
            AtomicReference<Long> idRecebidoNoSave = new AtomicReference<>();
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> {
                Atividade a = inv.getArgument(0);
                idRecebidoNoSave.set(a.getId()); // o que chegou no save, antes do "banco" gerar o id
                a.setId(99L);
                return a;
            });

            // o front manda "id": 555 de propósito: o controller deve ignorar e deixar o banco gerar
            String json = atividadeJson(555L, "Fracoes basicas", 10, "PENDENTE", 1L);

            mockMvc.perform(post("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(99))
                    .andExpect(jsonPath("$.titulo").value("Fracoes basicas"))
                    .andExpect(jsonPath("$.status").value("PENDENTE"));

            assertNull(idRecebidoNoSave.get(), "o id vindo do cliente tem que ser descartado antes de salvar");

            verify(logSistemaService).registrarPorUsername(
                    eq(USERNAME),
                    eq(AcaoLog.CRIAR_ATIVIDADE),
                    eq(EntidadeLog.ATIVIDADE),
                    eq(99L),
                    eq(ResultadoLog.SUCESSO),
                    anyString(),
                    any()
            );
        }

        @Test
        @DisplayName("400 quando a turma informada não existe: não salva e não registra log")
        void turmaInexistente() throws Exception {
            when(turmaRepository.existsById(1L)).thenReturn(false);

            mockMvc.perform(post("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(null, "Fracoes basicas", 10, "PENDENTE", 1L)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("BAD_REQUEST"));

            verify(atividadeRepository, never()).save(any());
            verifyNoInteractions(logSistemaService);
        }

        @Test
        @DisplayName("400 VALIDACAO_FALHOU quando o título tem menos de 5 caracteres")
        void tituloCurto() throws Exception {
            mockMvc.perform(post("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(null, "abc", 10, "PENDENTE", 1L)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                    .andExpect(jsonPath("$.campos.titulo").exists());

            verifyNoInteractions(turmaRepository, atividadeRepository, logSistemaService);
        }

        @Test
        @DisplayName("400 VALIDACAO_FALHOU quando a pontuação é menor que 1")
        void pontuacaoZero() throws Exception {
            mockMvc.perform(post("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(null, "Fracoes basicas", 0, "PENDENTE", 1L)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                    .andExpect(jsonPath("$.campos.pontuacao").exists());

            verifyNoInteractions(atividadeRepository);
        }

        @Test
        @DisplayName("400 REQUISICAO_INVALIDA quando o corpo não é um JSON válido")
        void jsonMalformado() throws Exception {
            mockMvc.perform(post("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{ isto nao e json"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

            verifyNoInteractions(atividadeRepository);
        }
    }

    // PUT
        @Nested
    @DisplayName("PUT /atividades")
    class Editar {

        @Test
        @DisplayName("PENDENTE -> EM_ANDAMENTO é uma publicação: registra PUBLICAR_ATIVIDADE (e não EDITAR)")
        void publicar() throws Exception {
            prepararEdicao(StatusAtividade.PENDENTE);

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Fracoes basicas", 10, "EM_ANDAMENTO", 1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_ANDAMENTO"));

            verify(logSistemaService).registrarPorUsername(
                    eq(USERNAME), eq(AcaoLog.PUBLICAR_ATIVIDADE), eq(EntidadeLog.ATIVIDADE),
                    eq(10L), eq(ResultadoLog.SUCESSO), anyString(), any());
            verify(logSistemaService, never()).registrarPorUsername(
                    any(), eq(AcaoLog.EDITAR_ATIVIDADE), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("edição comum (mesmo status) registra EDITAR_ATIVIDADE")
        void editarSemMudarStatus() throws Exception {
            prepararEdicao(StatusAtividade.EM_ANDAMENTO);

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Titulo editado", 10, "EM_ANDAMENTO", 1L)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.titulo").value("Titulo editado"));

            verify(logSistemaService).registrarPorUsername(
                    eq(USERNAME), eq(AcaoLog.EDITAR_ATIVIDADE), eq(EntidadeLog.ATIVIDADE),
                    eq(10L), eq(ResultadoLog.SUCESSO), anyString(), any());
            verify(logSistemaService, never()).registrarPorUsername(
                    any(), eq(AcaoLog.PUBLICAR_ATIVIDADE), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("EM_ANDAMENTO -> CONCLUIDO é edição, não publicação")
        void concluirNaoEPublicar() throws Exception {
            prepararEdicao(StatusAtividade.EM_ANDAMENTO);

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Fracoes basicas", 10, "CONCLUIDO", 1L)))
                    .andExpect(status().isOk());

            verify(logSistemaService).registrarPorUsername(
                    eq(USERNAME), eq(AcaoLog.EDITAR_ATIVIDADE), any(), any(), any(), any(), any());
            verify(logSistemaService, never()).registrarPorUsername(
                    any(), eq(AcaoLog.PUBLICAR_ATIVIDADE), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("a edição preserva as perguntas existentes (o body do PUT não as carrega)")
        void preservaPerguntas() throws Exception {
            Atividade existente = atividadeSalva(10L, "Fracoes basicas", StatusAtividade.EM_ANDAMENTO);
            Pergunta p1 = new Pergunta();
            p1.setId(1L);
            Pergunta p2 = new Pergunta();
            p2.setId(2L);
            existente.getPerguntas().add(p1);
            existente.getPerguntas().add(p2);

            when(atividadeRepository.findById(10L)).thenReturn(Optional.of(existente));
            when(turmaRepository.existsById(1L)).thenReturn(true);
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Fracoes basicas", 10, "EM_ANDAMENTO", 1L)))
                    .andExpect(status().isOk());

            ArgumentCaptor<Atividade> captor = ArgumentCaptor.forClass(Atividade.class);
            verify(atividadeRepository).save(captor.capture());
            assertSame(existente.getPerguntas(), captor.getValue().getPerguntas());
            assertEquals(2, captor.getValue().getPerguntas().size());
        }

        @Test
        @DisplayName("404 quando a atividade não existe: não salva e não registra log")
        void naoExiste() throws Exception {
            when(atividadeRepository.findById(10L)).thenReturn(Optional.empty());

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Fracoes basicas", 10, "EM_ANDAMENTO", 1L)))
                    .andExpect(status().isNotFound());

            verify(atividadeRepository, never()).save(any());
            verifyNoInteractions(logSistemaService);
        }

        @Test
        @DisplayName("400 quando a nova turma não existe: não salva e não registra log")
        void turmaInexistente() throws Exception {
            when(atividadeRepository.findById(10L))
                    .thenReturn(Optional.of(atividadeSalva(10L, "Fracoes basicas", StatusAtividade.EM_ANDAMENTO)));
            when(turmaRepository.existsById(1L)).thenReturn(false);

            mockMvc.perform(put("/atividades")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(atividadeJson(10L, "Fracoes basicas", 10, "EM_ANDAMENTO", 1L)))
                    .andExpect(status().isBadRequest());

            verify(atividadeRepository, never()).save(any());
            verifyNoInteractions(logSistemaService);
        }

        private void prepararEdicao(StatusAtividade statusAtual) {
            when(atividadeRepository.findById(10L))
                    .thenReturn(Optional.of(atividadeSalva(10L, "Fracoes basicas", statusAtual)));
            when(turmaRepository.existsById(1L)).thenReturn(true);
            when(atividadeRepository.save(any(Atividade.class))).thenAnswer(inv -> inv.getArgument(0));
        }
    }

    @Nested
    @DisplayName("PUT /atividades/{id}/progresso")
    class SalvarProgresso {

        @Test
        @DisplayName("200: repassa atividade, username e resposta ao service e devolve o progresso")
        void salvaResposta() throws Exception {
            when(progressoAtividadeService.salvarResposta(eq(10L), eq(USERNAME), any(SalvarRespostaRequest.class)))
                    .thenReturn(new ProgressoAtividadeResponse(
                            50, 1, 2, List.of(new RespostaProgressoResponse(1L, 2L))));

            mockMvc.perform(put("/atividades/10/progresso")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"perguntaId\":1,\"alternativaId\":2}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.percentualConclusao").value(50))
                    .andExpect(jsonPath("$.respondidas").value(1))
                    .andExpect(jsonPath("$.totalQuestoes").value(2))
                    .andExpect(jsonPath("$.respostas[0].perguntaId").value(1))
                    .andExpect(jsonPath("$.respostas[0].alternativaId").value(2));

            ArgumentCaptor<SalvarRespostaRequest> captor = ArgumentCaptor.forClass(SalvarRespostaRequest.class);
            verify(progressoAtividadeService).salvarResposta(eq(10L), eq(USERNAME), captor.capture());
            assertEquals(1L, captor.getValue().perguntaId());
            assertEquals(2L, captor.getValue().alternativaId());
        }

        @Test
        @DisplayName("400 VALIDACAO_FALHOU quando falta o alternativaId: o service nem é chamado")
        void semAlternativa() throws Exception {
            mockMvc.perform(put("/atividades/10/progresso")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"perguntaId\":1}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                    .andExpect(jsonPath("$.campos.alternativaId").exists());

            verifyNoInteractions(progressoAtividadeService);
        }

        @Test
        @DisplayName("propaga o erro de negócio do service (ex.: 409 atividade já concluída)")
        void erroDeNegocioDoService() throws Exception {
            when(progressoAtividadeService.salvarResposta(eq(10L), eq(USERNAME), any(SalvarRespostaRequest.class)))
                    .thenThrow(ApiException.conflito("ATIVIDADE_JA_CONCLUIDA", "Atividade ja concluida."));

            mockMvc.perform(put("/atividades/10/progresso")
                            .principal(autenticado)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"perguntaId\":1,\"alternativaId\":2}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.codigo").value("ATIVIDADE_JA_CONCLUIDA"));
        }
    }

    // DELETE
    @Nested
    @DisplayName("DELETE /atividades/{id}")
    class Excluir {

        @Test
        @DisplayName("204: apaga e registra o log EXCLUIR_ATIVIDADE")
        void excluiERegistraLog() throws Exception {
            when(atividadeRepository.findById(10L))
                    .thenReturn(Optional.of(atividadeSalva(10L, "Fracoes basicas", StatusAtividade.EM_ANDAMENTO)));

            mockMvc.perform(delete("/atividades/10").principal(autenticado))
                    .andExpect(status().isNoContent());

            verify(atividadeRepository).deleteById(10L);
            verify(logSistemaService).registrarPorUsername(
                    eq(USERNAME), eq(AcaoLog.EXCLUIR_ATIVIDADE), eq(EntidadeLog.ATIVIDADE),
                    eq(10L), eq(ResultadoLog.SUCESSO), anyString(), any());
        }

        @Test
        @DisplayName("404: atividade inexistente não é apagada e não gera log")
        void inexistente() throws Exception {
            when(atividadeRepository.findById(10L)).thenReturn(Optional.empty());

            mockMvc.perform(delete("/atividades/10").principal(autenticado))
                    .andExpect(status().isNotFound());

            verify(atividadeRepository, never()).deleteById(any());
            verifyNoInteractions(logSistemaService);
        }
    }

    private static Atividade atividadeSalva(Long id, String titulo, StatusAtividade status) {
        Atividade a = new Atividade();
        a.setId(id);
        a.setTitulo(titulo);
        a.setDescricao("Descricao da atividade " + id);
        a.setPontuacao(10);
        a.setStatus(status);
        Turma turma = new Turma();
        turma.setId(1L);
        turma.setNome("Turma A");
        a.setTurma(turma);
        return a;
    }

    private static String atividadeJson(Long id, String titulo, int pontuacao, String status, long turmaId) {
        String campoId = id == null ? "" : "\"id\":" + id + ",";
        return "{" + campoId
                + "\"titulo\":\"" + titulo + "\","
                + "\"descricao\":\"Descricao longa o bastante para validar\","
                + "\"pontuacao\":" + pontuacao + ","
                + "\"status\":\"" + status + "\","
                + "\"turma\":{\"id\":" + turmaId + "}"
                + "}";
    }
}

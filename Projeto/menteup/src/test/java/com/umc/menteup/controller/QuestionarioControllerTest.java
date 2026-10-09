package com.umc.menteup.controller;

import com.umc.menteup.dto.QuestionarioRequest;
import com.umc.menteup.exception.ApiException;
import com.umc.menteup.exception.GlobalExceptionHandler;
import com.umc.menteup.model.Alternativa;
import com.umc.menteup.model.Atividade;
import com.umc.menteup.model.Pergunta;
import com.umc.menteup.model.StatusAtividade;
import com.umc.menteup.model.Turma;
import com.umc.menteup.service.LogSistemaService;
import com.umc.menteup.service.QuestionarioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class QuestionarioControllerTest {

    private static final String USERNAME = "prof@teste.com";
    private static final String URL = "/atividades/10/questionario";

    @Mock private QuestionarioService questionarioService;
    @Mock private LogSistemaService logSistemaService;

    @InjectMocks
    private QuestionarioController controller;

    private MockMvc mockMvc;
    private Authentication autenticado;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler(logSistemaService))
                .build();
        autenticado = new UsernamePasswordAuthenticationToken(USERNAME, null, List.of());
    }

    @Test
    @DisplayName("200: repassa o corpo e o username ao service e devolve o questionário montado")
    void cadastraEDevolveQuestionario() throws Exception {
        when(questionarioService.cadastrarQuesitonario(eq(10L), any(QuestionarioRequest.class), eq(USERNAME)))
                .thenReturn(atividadeComUmaPergunta());

        mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONARIO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.atividade.id").value(10))
                .andExpect(jsonPath("$.atividade.titulo").value("Atividade de soma"))
                .andExpect(jsonPath("$.atividade.turma.nome").value("Turma A"))
                .andExpect(jsonPath("$.perguntas.length()").value(1))
                .andExpect(jsonPath("$.perguntas[0].id").value(101))
                .andExpect(jsonPath("$.perguntas[0].texto").value("Quanto e 1 + 1?"))
                .andExpect(jsonPath("$.perguntas[0].alternativas.length()").value(2))
                .andExpect(jsonPath("$.perguntas[0].alternativas[1].texto").value("2"))
                .andExpect(jsonPath("$.meuResultado").doesNotExist())
                .andExpect(jsonPath("$.meuProgresso").doesNotExist());
    }

    @Test
    @DisplayName("a resposta nunca expõe o gabarito (campo 'correta' das alternativas)")
    void naoVazaGabarito() throws Exception {
        when(questionarioService.cadastrarQuesitonario(eq(10L), any(QuestionarioRequest.class), eq(USERNAME)))
                .thenReturn(atividadeComUmaPergunta());

        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONARIO_VALIDO))
                .andExpect(status().isOk())
                .andReturn();

        assertFalse(resultado.getResponse().getContentAsString().contains("correta"),
                "o JSON de resposta não pode conter o campo 'correta'");
    }

    @Test
    @DisplayName("converte o JSON recebido no QuestionarioRequest esperado pelo service")
    void convertePayloadParaRequest() throws Exception {
        when(questionarioService.cadastrarQuesitonario(eq(10L), any(QuestionarioRequest.class), eq(USERNAME)))
                .thenReturn(atividadeComUmaPergunta());

        mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONARIO_VALIDO))
                .andExpect(status().isOk());

        ArgumentCaptor<QuestionarioRequest> captor = ArgumentCaptor.forClass(QuestionarioRequest.class);
        verify(questionarioService).cadastrarQuesitonario(eq(10L), captor.capture(), eq(USERNAME));

        QuestionarioRequest request = captor.getValue();
        assertEquals(1, request.perguntas().size());
        assertEquals("Quanto e 1 + 1?", request.perguntas().get(0).enunciado());
        assertEquals(2, request.perguntas().get(0).alternativas().size());
        assertEquals("1", request.perguntas().get(0).alternativas().get(0).texto());
        assertFalse(request.perguntas().get(0).alternativas().get(0).correta());
        assertTrue(request.perguntas().get(0).alternativas().get(1).correta());
    }

    // =========================================================================
    // Validação do corpo (o service nunca deve ser chamado)
    // =========================================================================

    @Test
    @DisplayName("400 VALIDACAO_FALHOU quando a lista de perguntas está vazia")
    void listaDePerguntasVazia() throws Exception {
        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"perguntas\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andReturn();

        assertTrue(resultado.getResponse().getContentAsString().contains("perguntas"));
        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 VALIDACAO_FALHOU quando o enunciado da pergunta está em branco")
    void enunciadoEmBranco() throws Exception {
        String json = """
                {"perguntas":[{"enunciado":"   ","alternativas":[
                    {"texto":"1","correta":false},{"texto":"2","correta":true}]}]}
                """;

        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andReturn();

        assertTrue(resultado.getResponse().getContentAsString().contains("perguntas[0].enunciado"));
        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 VALIDACAO_FALHOU quando a pergunta não tem alternativas")
    void perguntaSemAlternativas() throws Exception {
        String json = "{\"perguntas\":[{\"enunciado\":\"Quanto e 1 + 1?\",\"alternativas\":[]}]}";

        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andReturn();

        assertTrue(resultado.getResponse().getContentAsString().contains("perguntas[0].alternativas"));
        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 VALIDACAO_FALHOU quando o texto da alternativa está vazio")
    void textoDaAlternativaVazio() throws Exception {
        String json = """
                {"perguntas":[{"enunciado":"Quanto e 1 + 1?","alternativas":[
                    {"texto":"","correta":false},{"texto":"2","correta":true}]}]}
                """;

        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andReturn();

        assertTrue(resultado.getResponse().getContentAsString().contains("perguntas[0].alternativas[0].texto"));
        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 VALIDACAO_FALHOU quando a alternativa não informa se é correta")
    void alternativaSemCampoCorreta() throws Exception {
        String json = """
                {"perguntas":[{"enunciado":"Quanto e 1 + 1?","alternativas":[
                    {"texto":"1"},{"texto":"2","correta":true}]}]}
                """;

        MvcResult resultado = mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACAO_FALHOU"))
                .andReturn();

        assertTrue(resultado.getResponse().getContentAsString().contains("perguntas[0].alternativas[0].correta"));
        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 REQUISICAO_INVALIDA quando o corpo não é um JSON válido")
    void jsonMalformado() throws Exception {
        mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto nao e json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("400 REQUISICAO_INVALIDA quando o corpo está ausente")
    void semCorpo() throws Exception {
        mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));

        verifyNoInteractions(questionarioService);
    }

    @Test
    @DisplayName("propaga erro de negócio do service (ex.: 404 ATIVIDADE_NAO_ENCONTRADA)")
    void propagaErroDeNegocio() throws Exception {
        when(questionarioService.cadastrarQuesitonario(eq(10L), any(QuestionarioRequest.class), eq(USERNAME)))
                .thenThrow(ApiException.naoEncontrado("ATIVIDADE_NAO_ENCONTRADA", "Atividade nao encontrada."));

        mockMvc.perform(post(URL)
                        .principal(autenticado)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(QUESTIONARIO_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("ATIVIDADE_NAO_ENCONTRADA"));
    }

    private static final String QUESTIONARIO_VALIDO = """
            {"perguntas":[{"enunciado":"Quanto e 1 + 1?","alternativas":[
                {"texto":"1","correta":false},{"texto":"2","correta":true}]}]}
            """;

    /** Atividade como o service a devolve depois de salvar (com ids gerados). */
    private static Atividade atividadeComUmaPergunta() {
        Turma turma = new Turma();
        turma.setId(5L);
        turma.setNome("Turma A");

        Atividade atividade = new Atividade();
        atividade.setId(10L);
        atividade.setTitulo("Atividade de soma");
        atividade.setDescricao("Descricao da atividade de soma");
        atividade.setPontuacao(10);
        atividade.setStatus(StatusAtividade.EM_ANDAMENTO);
        atividade.setTurma(turma);

        Pergunta pergunta = new Pergunta();
        pergunta.setId(101L);
        pergunta.setEnunciado("Quanto e 1 + 1?");
        pergunta.setAtividade(atividade);

        Alternativa errada = new Alternativa();
        errada.setId(1011L);
        errada.setTexto("1");
        errada.setCorreta(false);
        errada.setPergunta(pergunta);

        Alternativa certa = new Alternativa();
        certa.setId(1012L);
        certa.setTexto("2");
        certa.setCorreta(true);
        certa.setPergunta(pergunta);

        pergunta.getAlternativas().add(errada);
        pergunta.getAlternativas().add(certa);
        atividade.getPerguntas().add(pergunta);
        return atividade;
    }
}

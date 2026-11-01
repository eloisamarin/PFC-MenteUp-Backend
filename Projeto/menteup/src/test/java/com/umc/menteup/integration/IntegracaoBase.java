package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClient;

import com.umc.menteup.repository.Codigo2FARepository;
import com.umc.menteup.service.EmailService;

/**
 * Base dos testes de integração: sobe a aplicação inteira em uma porta aleatória e
 * faz requisições HTTP reais com RestClient, passando por segurança, controller,
 * service, repository e banco H2.
 *
 * Banco próprio (menteup_integracao) e dados únicos por execução (System.nanoTime()):
 * nenhum teste depende de outro nem da ordem. Só o EmailService é mockado, por ser externo.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "Banco da Integracao" /* Banco  (menteup_integracao) e dados únicos por execução (System.nanoTime()):
        * nenhum teste depende de outro nem da ordem. Só o EmailService é mockado, por ser externo.
 */
)
@ActiveProfiles("test")
abstract class IntegracaoBase {

    protected static final String SENHA = ""; /* Inserir a senha */

    protected static final ParameterizedTypeReference<Map<String, Object>> JSON =
            new ParameterizedTypeReference<>() {
            };

    protected static final ParameterizedTypeReference<List<Map<String, Object>>> LISTA_JSON =
            new ParameterizedTypeReference<>() {
            };

    @LocalServerPort
    private int port;

    @Autowired
    protected Codigo2FARepository codigo2FARepository;

    @MockitoBean
    protected EmailService emailService; // serviço externo: nenhum e-mail real é enviado

    protected RestClient client;

    /** Usuário cadastrado e autenticado pelos endpoints reais. */
    protected record Sessao(Long id, String email, String token) {
    }

    /** Pergunta criada pelo endpoint de questionário, com os ids das alternativas. */
    protected record PerguntaCriada(Long id, List<Long> alternativas) {
    }

    @BeforeEach
    void configurarCliente() {
        client = RestClient.builder()
                .baseUrl("http://localhost:" + port)
                .defaultStatusHandler(status -> true, (req, res) -> {
                }) // 4xx e 5xx viram asserções, e não exceções
                .build();
    }

    protected static String emailUnico(String prefixo) {
        return prefixo + "." + System.nanoTime() + "@teste.com";
    }

    protected static String nomeUnico(String prefixo) {
        return prefixo + " " + System.nanoTime();
    }

    protected static Long id(Map<String, Object> json) {
        return ((Number) json.get("id")).longValue();
    }

    // ----------------------------------------------------------------- Usuários

    protected static Map<String, Object> buildUsuario(String nome, String email, String tipo) {
        Map<String, Object> usuario = new HashMap<>();
        usuario.put("nomeUsuario", nome);
        usuario.put("usuario", email);
        usuario.put("senha", SENHA);
        usuario.put("tipoUsuario", tipo);
        return usuario;
    }

    protected Long cadastrarUsuario(String email, String tipo) {
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Usuário Teste", email, tipo))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return id(resposta.getBody());
    }

    /** Faz os dois passos do login (senha + código 2FA) e devolve "Bearer ...". */
    protected String autenticar(String email) {
        ResponseEntity<Map<String, Object>> login = client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toEntity(JSON);

        assertThat(login.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Map<String, Object>> validacao = client.post().uri("/usuarios/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigo2FAPendente(email)))
                .retrieve()
                .toEntity(JSON);

        assertThat(validacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (String) validacao.getBody().get("token");
    }

    protected String codigo2FAPendente(String email) {
        return codigo2FARepository.findTopByUsuarioAndUtilizadoFalseOrderByIdDesc(email)
                .orElseThrow()
                .getCodigo();
    }

    protected Sessao novaSessao(String tipo) {
        String email = emailUnico(tipo.toLowerCase());
        Long id = cadastrarUsuario(email, tipo);
        return new Sessao(id, email, autenticar(email));
    }

    // ------------------------------------------------------------------- Turmas

    protected Long criarTurma(Sessao autor) {
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/turmas")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("nome", nomeUnico("Turma")))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return id(resposta.getBody());
    }

    protected void vincularAluno(Sessao autor, Long turmaId, Long alunoId) {
        ResponseEntity<Void> resposta = client.post().uri("/turmas/{turmaId}/alunos/{alunoId}", turmaId, alunoId)
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    // --------------------------------------------------------------- Atividades

    protected static Map<String, Object> buildAtividade(String titulo, Long turmaId, String status) {
        Map<String, Object> atividade = new HashMap<>();
        atividade.put("titulo", titulo);
        atividade.put("descricao", "Descrição da atividade de teste");
        atividade.put("pontuacao", 10);
        atividade.put("status", status);
        atividade.put("turma", Map.of("id", turmaId));
        return atividade;
    }

    protected Long criarAtividade(Sessao autor, Long turmaId, String status) {
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/atividades")
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildAtividade(nomeUnico("Atividade"), turmaId, status))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return id(resposta.getBody());
    }

    /** Cadastra as perguntas com duas alternativas cada; a primeira alternativa é a correta. */
    @SuppressWarnings("unchecked")
    protected List<PerguntaCriada> cadastrarQuestionario(Sessao autor, Long atividadeId, int quantidadePerguntas) {
        List<Map<String, Object>> perguntas = new java.util.ArrayList<>();
        for (int i = 1; i <= quantidadePerguntas; i++) {
            perguntas.add(Map.of(
                    "enunciado", "Pergunta " + i,
                    "alternativas", List.of(
                            Map.of("texto", "Certa " + i, "correta", true),
                            Map.of("texto", "Errada " + i, "correta", false))));
        }

        ResponseEntity<Map<String, Object>> resposta = client.post()
                .uri("/atividades/{id}/questionario", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, autor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntas", perguntas))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);

        return ((List<Map<String, Object>>) resposta.getBody().get("perguntas")).stream()
                .map(pergunta -> new PerguntaCriada(
                        id(pergunta),
                        ((List<Map<String, Object>>) pergunta.get("alternativas")).stream()
                                .map(IntegracaoBase::id)
                                .toList()))
                .toList();
    }

    protected ResponseEntity<Map<String, Object>> responder(Sessao aluno, Long atividadeId, Long perguntaId, Long alternativaId) {
        return client.put().uri("/atividades/{id}/progresso", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("perguntaId", perguntaId, "alternativaId", alternativaId))
                .retrieve()
                .toEntity(JSON);
    }

    protected ResponseEntity<Map<String, Object>> buscarAtividade(Sessao usuario, Long atividadeId) {
        return client.get().uri("/atividades/{id}", atividadeId)
                .header(HttpHeaders.AUTHORIZATION, usuario.token())
                .retrieve()
                .toEntity(JSON);
    }
}

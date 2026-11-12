package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.UsuarioRepository;

class UsuarioControllerIntegrationTest extends IntegracaoBase {

    private static final String BASE_PATH = "/usuarios";

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void cadastrar_comDadosValidos_deveRetornar201ComIdEGravarSenhaCriptografada() {
        // Arrange
        String email = emailUnico("novo");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Paulo Antunes", email, "ALUNO"))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();
        assertThat(resposta.getBody()).containsEntry("nomeUsuario", "Paulo Antunes")
                .containsEntry("usuario", email)
                .containsEntry("tipoUsuario", "ALUNO")
                .doesNotContainKey("senha");

        Usuario salvo = usuarioRepository.findById(id(resposta.getBody())).orElseThrow();
        assertThat(salvo.getUsuario()).isEqualTo(email);
        assertThat(passwordEncoder.matches(SENHA, salvo.getSenha())).isTrue();
    }

    @Test
    void cadastrar_comEmailDuplicado_deveRetornar400ENaoAlterarOOriginal() {
        // Arrange
        String email = emailUnico("duplicado");
        Long idOriginal = cadastrarUsuario(email, "ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Outro Nome", email, "PROFESSOR"))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        Usuario original = usuarioRepository.findByUsuario(email).orElseThrow();
        assertThat(original.getId()).isEqualTo(idOriginal);
        assertThat(original.getNomeUsuario()).isEqualTo("Usuário Teste");
        assertThat(original.getTipoUsuario()).isEqualTo("ALUNO");
    }

    @Test
    @SuppressWarnings("unchecked")
    void cadastrar_comEmailInvalidoESenhaCurta_deveRetornar400ComCamposInvalidos() {
        // Arrange
        Map<String, Object> invalido = buildUsuario("Paulo", "email-sem-arroba", "ALUNO");
        invalido.put("senha", "123");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalido)
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos"))
                .containsKeys("usuario", "senha")
                .containsEntry("senha", "A senha deve ter de 8 a 72 caracteres.");
    }

    @Test
    void cadastrar_comJsonMalformado_deveRetornar400ComCodigoRequisicaoInvalida() {
        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body("{ isto nao e json")
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "REQUISICAO_INVALIDA");
    }

    @Test
    void buscarPorId_comIdExistente_deveRetornar200ComOsDados() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.get().uri(BASE_PATH + "/{id}", sessao.id())
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(id(resposta.getBody())).isEqualTo(sessao.id());
        assertThat(resposta.getBody()).containsEntry("usuario", sessao.email());
    }

    @Test
    void buscarPorId_comIdInexistente_deveRetornar404() {
        // Arrange: só o administrador consulta outros ids (o aluno recebe 403 antes)
        Sessao sessao = novaSessao("ADMINISTRADOR");

        // Act
        ResponseEntity<Void> resposta = client.get().uri(BASE_PATH + "/{id}", Long.MAX_VALUE)
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void buscarPorId_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri(BASE_PATH + "/{id}", 1L)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void buscarPorId_comTokenInvalido_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.get().uri(BASE_PATH + "/{id}", 1L)
                .header(HttpHeaders.AUTHORIZATION, "Bearer token.invalido.aqui")
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void listarTodos_comToken_deveRetornar200ComUsuarioCadastrado() {
        // Arrange
        Sessao sessao = novaSessao("ADMINISTRADOR");
        String outroEmail = emailUnico("listado");
        cadastrarUsuario(outroEmail, "ALUNO");

        // Act
        ResponseEntity<List<Map<String, Object>>> resposta = client.get().uri(BASE_PATH + "/all")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(LISTA_JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).extracting(usuario -> usuario.get("usuario"))
                .contains(sessao.email(), outroEmail);
    }

    @Test
    void atualizar_comDadosValidos_deveRetornar200EGravarOsNovosDados() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");
        String novoEmail = emailUnico("atualizado");
        Map<String, Object> dados = buildUsuario("Nome Novo", novoEmail, "ALUNO");
        dados.put("id", sessao.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.put().uri(BASE_PATH + "/atualizar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("nomeUsuario", "Nome Novo")
                .containsEntry("usuario", novoEmail);

        Usuario salvo = usuarioRepository.findById(sessao.id()).orElseThrow();
        assertThat(salvo.getNomeUsuario()).isEqualTo("Nome Novo");
        assertThat(salvo.getUsuario()).isEqualTo(novoEmail);
    }

    @Test
    void atualizar_comIdInexistente_deveRetornar404() {
        // Arrange
        Sessao sessao = novaSessao("ADMINISTRADOR");
        Map<String, Object> dados = buildUsuario("Fantasma", emailUnico("fantasma"), "ALUNO");
        dados.put("id", Long.MAX_VALUE);

        // Act
        ResponseEntity<Void> resposta = client.put().uri(BASE_PATH + "/atualizar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void atualizar_comEmailDeOutroUsuario_deveRetornar400ComMensagem() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");
        String emailOcupado = emailUnico("ocupado");
        cadastrarUsuario(emailOcupado, "ALUNO");
        Map<String, Object> dados = buildUsuario("Nome", emailOcupado, "ALUNO");
        dados.put("id", sessao.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.put().uri(BASE_PATH + "/atualizar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("mensagem", "Usuário já existe!");
        assertThat(usuarioRepository.findById(sessao.id()).orElseThrow().getUsuario()).isEqualTo(sessao.email());
    }

    @Test
    void atualizar_semId_deveRetornar400ComCodigo() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");
        Map<String, Object> dados = buildUsuario("Sem Id", sessao.email(), "ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(sessao, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "ID_USUARIO_OBRIGATORIO");
    }

    @Test
    void atualizar_outroUsuarioComoAluno_deveRetornar403ENaoAlterar() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        Sessao vitima = novaSessao("ALUNO");
        Map<String, Object> dados = buildUsuario("Alterado", vitima.email(), "ADMINISTRADOR");
        dados.put("id", vitima.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(aluno, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROPRIO_USUARIO");
        Usuario salvo = usuarioRepository.findById(vitima.id()).orElseThrow();
        assertThat(salvo.getNomeUsuario()).isEqualTo("Usuário Teste");
        assertThat(salvo.getTipoUsuario()).isEqualTo("ALUNO");
    }

    @Test
    void atualizar_outroUsuarioComoProfessor_deveRetornar403ENaoAlterar() {
        // Arrange
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");
        Map<String, Object> dados = buildUsuario("Alterado", aluno.email(), "ALUNO");
        dados.put("id", aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(professor, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "SOMENTE_PROPRIO_USUARIO");
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getNomeUsuario())
                .isEqualTo("Usuário Teste");
    }

    @Test
    void atualizar_proprioTipoComoAluno_deveRetornar403ENaoPromover() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        Map<String, Object> dados = buildUsuario("Quero Ser Professor", aluno.email(), "PROFESSOR");
        dados.put("id", aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(aluno, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ALTERACAO_DE_TIPO_NEGADA");
        Usuario salvo = usuarioRepository.findById(aluno.id()).orElseThrow();
        assertThat(salvo.getTipoUsuario()).isEqualTo("ALUNO");
        assertThat(salvo.getNomeUsuario()).isEqualTo("Usuário Teste");
    }

    @Test
    void atualizar_outroUsuarioComoAdministrador_deveRetornar200EAlterarOTipo() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");
        Map<String, Object> dados = buildUsuario("Agora Professor", aluno.email(), "PROFESSOR");
        dados.put("id", aluno.id());

        // Act
        ResponseEntity<Map<String, Object>> resposta = atualizar(administrador, dados);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("tipoUsuario", "PROFESSOR");
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getTipoUsuario()).isEqualTo("PROFESSOR");
    }

    @Test
    void atualizar_semToken_deveRetornar401() {
        // Arrange
        Map<String, Object> dados = buildUsuario("Sem Token", emailUnico("semtoken"), "ALUNO");
        dados.put("id", 1L);

        // Act
        ResponseEntity<Void> resposta = client.put().uri(BASE_PATH + "/atualizar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logar_comCredenciaisValidas_deveRetornar200SemTokenEEnviarCodigo2FA() {
        // Arrange
        String email = emailUnico("login");
        cadastrarUsuario(email, "ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).containsEntry("usuario", email);
        assertThat(resposta.getBody().get("token")).isNull();
        verify(emailService).enviarCodigo2FA(email, codigo2FAPendente(email));
    }

    @Test
    void logar_comSenhaErrada_deveRetornar401ENaoEnviarCodigo() {
        // Arrange
        String email = emailUnico("senhaerrada");
        cadastrarUsuario(email, "ALUNO");

        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH + "/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", "senha-incorreta"))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(emailService, never()).enviarCodigo2FA(eq(email), any());
    }

    @Test
    void validar2fa_comCodigoCorreto_deveRetornar200ComTokenQueAcessaRotaProtegida() {
        // Arrange
        String email = emailUnico("doisfatores");
        Long id = cadastrarUsuario(email, "PROFESSOR");
        client.post().uri(BASE_PATH + "/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigo2FAPendente(email)))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(id(resposta.getBody())).isEqualTo(id);
        assertThat(resposta.getBody()).containsEntry("tipoUsuario", "PROFESSOR");
        String token = (String) resposta.getBody().get("token");
        assertThat(token).startsWith("Bearer ");

        ResponseEntity<Void> rotaProtegida = client.get().uri(BASE_PATH + "/{id}", id)
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .toBodilessEntity();
        assertThat(rotaProtegida.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void validar2fa_comCodigoErrado_deveRetornar401() {
        // Arrange
        String email = emailUnico("codigoerrado");
        cadastrarUsuario(email, "ALUNO");
        client.post().uri(BASE_PATH + "/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();
        String codigoErrado = codigo2FAPendente(email).equals("000000") ? "111111" : "000000";

        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH + "/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigoErrado))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void validar2fa_comCodigoJaUtilizado_deveRetornar401() {
        // Arrange
        String email = emailUnico("reuso");
        cadastrarUsuario(email, "ALUNO");
        client.post().uri(BASE_PATH + "/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();
        Map<String, String> validacao = Map.of("usuario", email, "codigo", codigo2FAPendente(email));
        client.post().uri(BASE_PATH + "/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validacao)
                .retrieve()
                .toBodilessEntity();

        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH + "/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(validacao)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void registrarFcmToken_comTokenValido_deveRetornar200EGravarNoUsuario() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");

        // Act
        ResponseEntity<String> resposta = client.post().uri(BASE_PATH + "/fcm-token")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fcmToken", "token-do-dispositivo"))
                .retrieve()
                .toEntity(String.class);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(resposta.getBody()).isEqualTo("Token Firebase registrado com sucesso.");
        assertThat(usuarioRepository.findById(sessao.id()).orElseThrow().getFcmToken())
                .isEqualTo("token-do-dispositivo");
    }

    private ResponseEntity<Map<String, Object>> atualizar(Sessao sessao, Map<String, Object> dados) {
        return client.put().uri(BASE_PATH + "/atualizar")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);
    }

    @Test
    @SuppressWarnings("unchecked")
    void registrarFcmToken_comTokenEmBranco_deveRetornar400ComCampoInvalido() {
        // Arrange
        Sessao sessao = novaSessao("ALUNO");

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri(BASE_PATH + "/fcm-token")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fcmToken", " "))
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
        assertThat((Map<String, String>) resposta.getBody().get("campos")).containsKey("fcmToken");
    }

    @Test
    void registrarFcmToken_semToken_deveRetornar401() {
        // Act
        ResponseEntity<Void> resposta = client.post().uri(BASE_PATH + "/fcm-token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("fcmToken", "token-do-dispositivo"))
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}

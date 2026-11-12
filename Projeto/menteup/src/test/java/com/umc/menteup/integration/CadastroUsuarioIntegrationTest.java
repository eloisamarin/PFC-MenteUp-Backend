package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.repository.UsuarioRepository;

/*
 * Regras de criação de usuário:
 * - cadastro público (/usuarios/cadastrar): só ALUNO ou PROFESSOR;
 * - ADMINISTRADOR só é criado por outro administrador logado (POST /usuarios);
 * - o tipo precisa ser um dos 3 existentes (antes aceitava qualquer texto);
 * - o cadastro público exige o aceite do Termo de Aceite e da Política de Privacidade.
 */
class CadastroUsuarioIntegrationTest extends IntegracaoBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    // ------------------------------------------------------------- Cadastro público

    @Test
    void cadastroPublico_comoAlunoOuProfessor_deveCriar() {
        assertThat(cadastrarPublico("ALUNO").getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(cadastrarPublico("PROFESSOR").getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void cadastroPublico_comTipoEmMinusculas_deveSalvarEmMaiusculas() {
        ResponseEntity<Map<String, Object>> resposta = cadastrarPublico(" aluno ");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("tipoUsuario", "ALUNO");
    }

    @Test
    void cadastroPublico_comoAdministrador_deveRetornar403ENaoCriar() {
        String email = emailUnico("intruso");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Intruso", email, "ADMINISTRADOR"))
                .retrieve()
                .toEntity(JSON);

        // Assert: bloqueado, e a conta não existe (o login falha)
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "ADMINISTRADOR_SOMENTE_POR_ADMINISTRADOR");
        assertThat(logar(email)).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void cadastroPublico_comTipoInventado_deveRetornar400() {
        ResponseEntity<Map<String, Object>> resposta = cadastrarPublico("HACKER");

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "TIPO_USUARIO_INVALIDO");
    }

    @Test
    void cadastroPublico_semAceitarOsTermos_deveRetornar400ENaoCriar() {
        // Arrange
        String email = emailUnico("sem.termo");
        Map<String, Object> dados = new java.util.HashMap<>(buildUsuario("Sem Termo", email, "ALUNO"));
        dados.put("aceitouTermos", false);

        // Act
        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "TERMO_NAO_ACEITO");
        assertThat(usuarioRepository.findByUsuario(email)).isEmpty();
    }

    @Test
    void cadastroPublico_semInformarOAceite_deveRetornar400() {
        // Quem chama a API direto, sem o campo, também é recusado
        Map<String, Object> dados = new java.util.HashMap<>(buildUsuario("Sem Campo", emailUnico("sem.campo"), "ALUNO"));
        dados.remove("aceitouTermos");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "TERMO_NAO_ACEITO");
    }

    @Test
    void cadastroPublico_comAceite_deveGravarADataDoAceiteSemDevolverNoJson() {
        String email = emailUnico("com.termo");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Com Termo", email, "PROFESSOR"))
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).doesNotContainKey("aceitouTermos").doesNotContainKey("termoAceitoEm");
        assertThat(usuarioRepository.findByUsuario(email).orElseThrow().getTermoAceitoEm()).isNotNull();
    }

    @Test
    void atualizarUsuario_deveManterADataDoAceite() {
        // Arrange
        Sessao aluno = novaSessao("ALUNO");
        java.time.LocalDateTime aceite = usuarioRepository.findById(aluno.id()).orElseThrow().getTermoAceitoEm();
        Map<String, Object> dados = new java.util.HashMap<>(buildUsuario("Nome Novo", aluno.email(), "ALUNO"));
        dados.put("id", aluno.id());

        // Act
        ResponseEntity<Void> resposta = client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toBodilessEntity();

        // Assert
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(aceite).isNotNull();
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getTermoAceitoEm()).isEqualTo(aceite);
    }

    // ------------------------------------------------------------- Criação pelo administrador

    @Test
    void administrador_deveCriarOutroAdministradorQueConsegueEntrar() {
        // Arrange
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("novo.admin");

        // Act
        ResponseEntity<Map<String, Object>> resposta = criarPeloAdministrador(administrador, email, "ADMINISTRADOR");

        // Assert: criado; depois do primeiro acesso pelo convite, entra e acessa uma área só de admin
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).containsEntry("tipoUsuario", "ADMINISTRADOR").doesNotContainKey("senha");

        org.mockito.ArgumentCaptor<String> link = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(emailService)
                .enviarConvitePrimeiroAcesso(org.mockito.ArgumentMatchers.eq(email), org.mockito.ArgumentMatchers.anyString(), link.capture());
        String convite = link.getValue().substring(link.getValue().indexOf("token=") + 6);
        ResponseEntity<Void> primeiroAcesso = client.post().uri("/usuarios/senha/primeiro-acesso")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("token", convite, "novaSenha", SENHA, "aceitouTermos", true))
                .retrieve()
                .toBodilessEntity();
        assertThat(primeiroAcesso.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        String token = autenticar(email);
        ResponseEntity<Void> logs = client.get().uri("/logs")
                .header(HttpHeaders.AUTHORIZATION, token)
                .retrieve()
                .toBodilessEntity();
        assertThat(logs.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void administrador_deveCriarAlunoEProfessor() {
        Sessao administrador = novaSessao("ADMINISTRADOR");

        assertThat(criarPeloAdministrador(administrador, emailUnico("aluno"), "ALUNO").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(criarPeloAdministrador(administrador, emailUnico("prof"), "PROFESSOR").getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void professorEAluno_naoDevemCriarUsuarios() {
        Sessao professor = novaSessao("PROFESSOR");
        Sessao aluno = novaSessao("ALUNO");

        assertThat(criarPeloAdministrador(professor, emailUnico("x"), "ADMINISTRADOR").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(criarPeloAdministrador(aluno, emailUnico("y"), "ALUNO").getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void criarUsuario_semLogin_deveRetornar401() {
        ResponseEntity<Void> resposta = client.post().uri("/usuarios")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Sem login", emailUnico("z"), "ADMINISTRADOR"))
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void administrador_comEmailRepetidoOuTipoInvalido_deveRecusar() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        String email = emailUnico("repetido");
        criarPeloAdministrador(administrador, email, "ALUNO");

        ResponseEntity<Map<String, Object>> repetido = criarPeloAdministrador(administrador, email, "ALUNO");
        ResponseEntity<Map<String, Object>> tipoInvalido = criarPeloAdministrador(administrador, emailUnico("w"), "SUPERUSUARIO");

        assertThat(repetido.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(repetido.getBody()).containsEntry("codigo", "EMAIL_JA_CADASTRADO");
        assertThat(tipoInvalido.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(tipoInvalido.getBody()).containsEntry("codigo", "TIPO_USUARIO_INVALIDO");
    }

    @Test
    void atualizar_comTipoInventado_deveRetornar400MesmoParaAdministrador() {
        Sessao administrador = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        Map<String, Object> dados = new java.util.HashMap<>(buildUsuario("Aluno", aluno.email(), "XPTO"));
        dados.put("id", aluno.id());

        ResponseEntity<Map<String, Object>> resposta = client.put().uri("/usuarios/atualizar")
                .header(HttpHeaders.AUTHORIZATION, administrador.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "TIPO_USUARIO_INVALIDO");
    }

    // ------------------------------------------------------------- Helpers

    private ResponseEntity<Map<String, Object>> cadastrarPublico(String tipo) {
        return client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Pessoa Teste", emailUnico("publico"), tipo))
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> criarPeloAdministrador(Sessao sessao, String email, String tipo) {
        return client.post().uri("/usuarios")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildUsuario("Criado pelo Admin", email, tipo))
                .retrieve()
                .toEntity(JSON);
    }

    private HttpStatus logar(String email) {
        return HttpStatus.valueOf(client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity()
                .getStatusCode()
                .value());
    }
}

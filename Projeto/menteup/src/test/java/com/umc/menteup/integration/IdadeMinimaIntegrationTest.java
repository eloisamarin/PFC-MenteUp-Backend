package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.repository.UsuarioRepository;

// O MenteUp é para alunos com 12 anos ou mais: a data de nascimento é conferida e não é guardada
class IdadeMinimaIntegrationTest extends IntegracaoBase {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private Clock clock;

    @Test
    void alunoComMenosDe12Anos_naoConsegueSeCadastrar() {
        String email = emailUnico("onze.anos");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, "ALUNO", hoje().minusYears(12).plusDays(1));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "IDADE_MINIMA");
        assertThat(usuarioRepository.findByUsuario(email)).isEmpty();
    }

    @Test
    void alunoQueFaz12AnosHoje_seCadastraEJaSaiConfirmado() {
        String email = emailUnico("doze.anos");

        ResponseEntity<Map<String, Object>> resposta = cadastrar(email, "ALUNO", hoje().minusYears(12));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(resposta.getBody()).doesNotContainKey("dataNascimento");
        assertThat(usuarioRepository.findByUsuario(email).orElseThrow().getIdadeMinimaConfirmadaEm()).isNotNull();
        assertThat(login(email)).containsEntry("idadePendente", false);
    }

    @Test
    void alunoSemDataOuComDataImpossivel_eRecusado() {
        ResponseEntity<Map<String, Object>> semData = cadastrar(emailUnico("sem.data"), "ALUNO", null);
        assertThat(semData.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(semData.getBody()).containsEntry("codigo", "DATA_NASCIMENTO_OBRIGATORIA");

        ResponseEntity<Map<String, Object>> futura = cadastrar(emailUnico("futura"), "ALUNO", hoje().plusDays(1));
        assertThat(futura.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(futura.getBody()).containsEntry("codigo", "DATA_NASCIMENTO_INVALIDA");
    }

    @Test
    void professor_naoPrecisaInformarADataDeNascimento() {
        ResponseEntity<Map<String, Object>> resposta = cadastrar(emailUnico("professor"), "PROFESSOR", null);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void contaAntigaComIdadeMinima_confirmaEDeixaDeEstarPendente() {
        Sessao aluno = contaAntigaDeAluno();
        assertThat(login(aluno.email())).containsEntry("idadePendente", true);

        ResponseEntity<Map<String, Object>> resposta = confirmar(aluno, hoje().minusYears(14));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getIdadeMinimaConfirmadaEm()).isNotNull();
        assertThat(login(aluno.email())).containsEntry("idadePendente", false);
        assertThat(logSistemaRepository.findAll())
                .anyMatch(l -> l.getAcao() == AcaoLog.CONFIRMAR_IDADE && aluno.id().equals(l.getEntidadeId()));
    }

    @Test
    void contaAntigaComMenosDe12Anos_eExcluida() {
        Sessao aluno = contaAntigaDeAluno();

        ResponseEntity<Map<String, Object>> resposta = confirmar(aluno, hoje().minusYears(10));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resposta.getBody()).containsEntry("codigo", "IDADE_MINIMA");
        Usuario excluida = usuarioRepository.findById(aluno.id()).orElseThrow();
        assertThat(excluida.getUsuario()).endsWith("@menteup.invalid");
        assertThat(logSistemaRepository.findAll())
                .anyMatch(l -> l.getAcao() == AcaoLog.EXCLUIR_POR_IDADE_MINIMA && aluno.id().equals(l.getEntidadeId()));
    }

    @Test
    void contaQueJaConfirmou_naoMudaAoInformarDeNovo() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = confirmar(aluno, hoje().minusYears(10));

        // A idade já foi confirmada no cadastro: um novo envio não exclui a conta
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(usuarioRepository.findById(aluno.id()).orElseThrow().getUsuario()).isEqualTo(aluno.email());
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    // Aluno criado antes da regra: sem a confirmação da idade gravada
    private Sessao contaAntigaDeAluno() {
        Sessao aluno = novaSessao("ALUNO");
        Usuario conta = usuarioRepository.findById(aluno.id()).orElseThrow();
        conta.setIdadeMinimaConfirmadaEm(null);
        usuarioRepository.save(conta);
        return aluno;
    }

    private ResponseEntity<Map<String, Object>> cadastrar(String email, String tipo, LocalDate nascimento) {
        Map<String, Object> dados = new HashMap<>(buildUsuario("Pessoa Idade", email, tipo));
        if (nascimento == null) {
            dados.remove("dataNascimento");
        } else {
            dados.put("dataNascimento", nascimento.toString());
        }
        return client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(dados)
                .retrieve()
                .toEntity(JSON);
    }

    private ResponseEntity<Map<String, Object>> confirmar(Sessao sessao, LocalDate nascimento) {
        return client.post().uri("/usuarios/me/idade")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("dataNascimento", nascimento.toString()))
                .retrieve()
                .toEntity(JSON);
    }

    // Resposta completa do login (depois do código 2FA)
    private Map<String, Object> login(String email) {
        client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();
        return client.post().uri("/usuarios/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigo2FAPendente(email)))
                .retrieve()
                .toEntity(JSON)
                .getBody();
    }
}

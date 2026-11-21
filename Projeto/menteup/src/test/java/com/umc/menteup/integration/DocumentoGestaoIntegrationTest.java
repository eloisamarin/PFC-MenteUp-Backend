package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.AcaoLog;
import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.repository.AceiteDocumentoRepository;
import com.umc.menteup.repository.DocumentoLegalRepository;
import com.umc.menteup.repository.LogSistemaRepository;
import com.umc.menteup.service.DocumentoLegalService;

// O administrador publica e cancela versões do Termo e da Política pela API
class DocumentoGestaoIntegrationTest extends IntegracaoBase {

    @Autowired
    private DocumentoLegalService documentoLegalService;

    @Autowired
    private DocumentoLegalRepository documentoRepository;

    @Autowired
    private AceiteDocumentoRepository aceiteRepository;

    @Autowired
    private LogSistemaRepository logSistemaRepository;

    @Autowired
    private Clock clock;

    // Versões criadas aqui saem no fim, para não mudar o documento vigente dos outros testes
    @AfterEach
    void removerVersoesDeTeste() {
        List<DocumentoLegal> deTeste = documentoRepository.findAll().stream()
                .filter(d -> d.getConteudo().contains("Texto de teste da gestão"))
                .toList();
        aceiteRepository.deleteAll(aceiteRepository.findAll().stream()
                .filter(a -> deTeste.stream().anyMatch(d -> d.getId().equals(a.getDocumento().getId())))
                .toList());
        documentoRepository.deleteAll(deTeste);
        documentoLegalService.recarregar();
    }

    @Test
    void publicarAgendada_naoMudaOVigenteEPodeSerCancelada() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        String vigenteAntes = documentoLegalService.vigente(TipoDocumento.POLITICA).getVersao();

        ResponseEntity<Map<String, Object>> publicada =
                publicar(admin, "politica", textoDeTeste("agendada"), hoje().plusDays(7));

        assertThat(publicada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String nova = (String) publicada.getBody().get("versao");
        assertThat(DocumentoLegal.compararVersoes(nova, vigenteAntes)).isPositive();
        assertThat(publicada.getBody()).containsEntry("situacao", "AGENDADA");

        // Para o público nada muda até a data
        assertThat(documentoLegalService.vigente(TipoDocumento.POLITICA).getVersao()).isEqualTo(vigenteAntes);

        // O administrador vê a agendada e o texto dela
        assertThat(listaDaGestao(admin, "politica")).first()
                .satisfies(linha -> assertThat(linha).containsEntry("versao", nova)
                        .containsEntry("situacao", "AGENDADA"));
        assertThat(client.get().uri("/documentos/gestao/politica/versoes/" + nova)
                .header(HttpHeaders.AUTHORIZATION, admin.token())
                .retrieve().toEntity(JSON).getBody()).containsKey("conteudo");

        // Só uma agendada por vez
        ResponseEntity<Map<String, Object>> segunda =
                publicar(admin, "politica", textoDeTeste("segunda"), hoje().plusDays(8));
        assertThat(segunda.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(segunda.getBody()).containsEntry("codigo", "VERSAO_AGENDADA_EXISTENTE");

        // Cancelar a agendada
        ResponseEntity<Void> cancelamento = client.delete().uri("/documentos/gestao/politica/versoes/" + nova)
                .header(HttpHeaders.AUTHORIZATION, admin.token())
                .retrieve().toBodilessEntity();
        assertThat(cancelamento.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(documentoRepository.findByTipoAndVersao("POLITICA", nova)).isEmpty();

        assertThat(logSistemaRepository.findAll())
                .anyMatch(l -> l.getAcao() == AcaoLog.PUBLICAR_DOCUMENTO)
                .anyMatch(l -> l.getAcao() == AcaoLog.CANCELAR_DOCUMENTO);
    }

    @Test
    void publicarComVigenciaHoje_passaAValerEFicaPendenteParaQuemJaAceitou() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> publicada =
                publicar(admin, "termo", textoDeTeste("hoje"), hoje());

        assertThat(publicada.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        String nova = (String) publicada.getBody().get("versao");
        assertThat(publicada.getBody()).containsEntry("situacao", "VIGENTE");
        assertThat(documentoLegalService.vigente(TipoDocumento.TERMO).getVersao()).isEqualTo(nova);
        assertThat(documentoLegalService.pendentes(aluno.id()))
                .extracting(DocumentoLegal::getVersao)
                .containsExactly(nova);

        // Versão que já está valendo não pode ser cancelada
        ResponseEntity<Map<String, Object>> cancelamento = client.delete()
                .uri("/documentos/gestao/termo/versoes/" + nova)
                .header(HttpHeaders.AUTHORIZATION, admin.token())
                .retrieve().toEntity(JSON);
        assertThat(cancelamento.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(cancelamento.getBody()).containsEntry("codigo", "VERSAO_JA_VIGENTE");
    }

    @Test
    void vigenciaNoPassado_eRecusada() {
        Sessao admin = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta =
                publicar(admin, "termo", textoDeTeste("passado"), hoje().minusDays(1));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VIGENCIA_INVALIDA");
    }

    @Test
    void textoComHtml_eRecusado() {
        Sessao admin = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = publicar(admin, "termo",
                textoDeTeste("html") + "\n<script>alert(1)</script>", hoje().plusDays(3));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "CONTEUDO_COM_HTML");
    }

    @Test
    void textoIgualAoVigente_eRecusado() {
        Sessao admin = novaSessao("ADMINISTRADOR");
        String atual = documentoLegalService.vigente(TipoDocumento.TERMO).getConteudo();

        ResponseEntity<Map<String, Object>> resposta = publicar(admin, "termo", atual, hoje().plusDays(3));

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "CONTEUDO_SEM_MUDANCA");
    }

    @Test
    void camposObrigatorios_saoConferidos() {
        Sessao admin = novaSessao("ADMINISTRADOR");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/documentos/gestao/termo/versoes")
                .header(HttpHeaders.AUTHORIZATION, admin.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("conteudo", ""))
                .retrieve().toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VALIDACAO_FALHOU");
    }

    @Test
    void quemNaoEAdministrador_naoPublica() {
        Sessao professor = novaSessao("PROFESSOR");

        Map<String, Object> corpo = Map.of("conteudo", textoDeTeste("professor"), "resumo", "Mudança de teste.",
                "vigenteDesde", hoje().plusDays(3).toString());
        ResponseEntity<Void> resposta = client.post().uri("/documentos/gestao/termo/versoes")
                .header(HttpHeaders.AUTHORIZATION, professor.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toBodilessEntity();

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(documentoRepository.findAll())
                .noneMatch(d -> d.getConteudo().contains("Texto de teste da gestão (professor)"));
    }

    private ResponseEntity<Map<String, Object>> publicar(Sessao sessao, String tipo, String conteudo,
                                                         LocalDate vigenteDesde) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("conteudo", conteudo);
        corpo.put("resumo", "Mudança de teste.");
        corpo.put("vigenteDesde", vigenteDesde.toString());
        corpo.put("exigeNovoAceite", true);
        return client.post().uri("/documentos/gestao/" + tipo + "/versoes")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);
    }

    private List<Map<String, Object>> listaDaGestao(Sessao sessao, String tipo) {
        return client.get().uri("/documentos/gestao/" + tipo + "/versoes")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .body(LISTA_JSON);
    }

    private LocalDate hoje() {
        return LocalDate.now(clock);
    }

    private static String textoDeTeste(String caso) {
        return "# Documento\n\nTexto de teste da gestão (" + caso + ").\n";
    }
}

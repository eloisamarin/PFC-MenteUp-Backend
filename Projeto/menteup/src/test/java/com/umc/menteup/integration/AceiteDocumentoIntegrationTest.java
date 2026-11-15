package com.umc.menteup.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.repository.AceiteDocumentoRepository;
import com.umc.menteup.repository.DocumentoLegalRepository;
import com.umc.menteup.service.DocumentoLegalService;

// Aceite do Termo e da Política: versão gravada no cadastro e novo aceite quando sai uma versão nova
class AceiteDocumentoIntegrationTest extends IntegracaoBase {

    @Autowired
    private AceiteDocumentoRepository aceiteRepository;

    @Autowired
    private DocumentoLegalRepository documentoRepository;

    @Autowired
    private DocumentoLegalService documentoLegalService;

    @Test
    void cadastro_comVersaoAntiga_deveRetornar409() {
        Map<String, Object> corpo = buildUsuario("Aluno", emailUnico("aluno"), "ALUNO");
        corpo.put("termoVersao", "0.9");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(resposta.getBody()).containsEntry("codigo", "DOCUMENTO_ATUALIZADO");
    }

    @Test
    void cadastro_comAVersaoVigente_deveRegistrarOAceiteDosDoisDocumentos() {
        Map<String, Object> corpo = buildUsuario("Aluno", emailUnico("aluno"), "ALUNO");
        corpo.put("termoVersao", documentoLegalService.vigente(TipoDocumento.TERMO).getVersao());
        corpo.put("politicaVersao", documentoLegalService.vigente(TipoDocumento.POLITICA).getVersao());

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/cadastrar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(corpo)
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(aceiteRepository.buscarDoUsuario(id(resposta.getBody()))).hasSize(2);
        // As versões só chegam na requisição: não voltam no JSON
        assertThat(resposta.getBody()).doesNotContainKeys("termoVersao", "politicaVersao");
    }

    @Test
    void contaEmDia_naoDeveTerPendencias() {
        // O cadastro dos testes não manda a versão (como o front antigo): vale a vigente
        Sessao aluno = novaSessao("ALUNO");

        Map<String, Object> situacao = situacao(aluno);

        assertThat((List<?>) situacao.get("pendentes")).isEmpty();
        assertThat((List<?>) situacao.get("aceites")).hasSize(2);
    }

    @Test
    void versaoNovaQueExigeAceite_deveFicarPendenteAteAPessoaAceitar() {
        Sessao aluno = novaSessao("ALUNO");
        DocumentoLegal nova = publicarTermo("99.0");

        try {
            // Pendente na consulta e na resposta do login
            assertThat(versoesPendentes(aluno)).containsExactly("99.0");
            assertThat(loginDevolvePendencia(aluno.email())).containsExactly("TERMO");

            // Aceitar uma versão que não é a vigente: recusado
            assertThat(aceitar(aluno, "1.0").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

            // Aceitar a vigente: fica em dia
            ResponseEntity<Map<String, Object>> aceite = aceitar(aluno, "99.0");
            assertThat(aceite.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat((List<?>) aceite.getBody().get("pendentes")).isEmpty();
            assertThat(versoesPendentes(aluno)).isEmpty();
        } finally {
            removerVersao(nova);
        }
    }

    @Test
    void aceitar_semVersao_deveRetornar400() {
        Sessao aluno = novaSessao("ALUNO");

        ResponseEntity<Map<String, Object>> resposta = client.post().uri("/usuarios/me/aceites")
                .header(HttpHeaders.AUTHORIZATION, aluno.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of())
                .retrieve()
                .toEntity(JSON);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).containsEntry("codigo", "VERSAO_NAO_INFORMADA");
    }

    // ------------------------------------------------------------------ Apoio

    // Versão nova só deste teste; removida no fim para não afetar os outros testes
    private DocumentoLegal publicarTermo(String versao) {
        DocumentoLegal nova = documentoRepository.save(new DocumentoLegal(TipoDocumento.TERMO, versao,
                "## Termo de teste\n", "0".repeat(64), LocalDate.now(), true, "Versão de teste."));
        documentoLegalService.recarregar();
        return nova;
    }

    private void removerVersao(DocumentoLegal versao) {
        aceiteRepository.deleteAll(aceiteRepository.findAll().stream()
                .filter(a -> a.getDocumento().getId().equals(versao.getId()))
                .toList());
        documentoRepository.deleteById(versao.getId());
        documentoLegalService.recarregar();
    }

    private Map<String, Object> situacao(Sessao sessao) {
        ResponseEntity<Map<String, Object>> resposta = client.get().uri("/usuarios/me/aceites")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .retrieve()
                .toEntity(JSON);
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.OK);
        return resposta.getBody();
    }

    @SuppressWarnings("unchecked")
    private List<String> versoesPendentes(Sessao sessao) {
        return ((List<Map<String, Object>>) situacao(sessao).get("pendentes")).stream()
                .map(p -> (String) p.get("versao"))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private List<String> loginDevolvePendencia(String email) {
        client.post().uri("/usuarios/logar")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "senha", SENHA))
                .retrieve()
                .toBodilessEntity();
        ResponseEntity<Map<String, Object>> validacao = client.post().uri("/usuarios/validar-2fa")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("usuario", email, "codigo", codigo2FAPendente(email)))
                .retrieve()
                .toEntity(JSON);
        assertThat(validacao.getStatusCode()).isEqualTo(HttpStatus.OK);
        return (List<String>) validacao.getBody().get("aceitesPendentes");
    }

    private ResponseEntity<Map<String, Object>> aceitar(Sessao sessao, String termoVersao) {
        return client.post().uri("/usuarios/me/aceites")
                .header(HttpHeaders.AUTHORIZATION, sessao.token())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("termoVersao", termoVersao))
                .retrieve()
                .toEntity(JSON);
    }
}

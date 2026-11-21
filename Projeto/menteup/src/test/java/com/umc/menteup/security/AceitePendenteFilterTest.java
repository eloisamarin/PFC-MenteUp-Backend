package com.umc.menteup.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.model.TipoDocumento;
import com.umc.menteup.model.Usuario;
import com.umc.menteup.service.DocumentoLegalService;

// Bloqueio de quem não aceitou a versão atual do Termo ou da Política (linha rígida)
class AceitePendenteFilterTest {

    private static final String EMAIL = "aluno@teste.com";

    private DocumentoLegalService documentoLegalService;

    @BeforeEach
    void entrarComoAluno() {
        documentoLegalService = mock(DocumentoLegalService.class);

        Usuario aluno = new Usuario();
        aluno.setId(1L);
        aluno.setUsuario(EMAIL);
        aluno.setSenha("hash");
        aluno.setTipoUsuario("ALUNO");
        aluno.setIdadeMinimaConfirmadaEm(java.time.LocalDateTime.now());
        UserDetailsImpl usuario = new UserDetailsImpl(aluno);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
    }

    @AfterEach
    void sair() {
        SecurityContextHolder.clearContext();
    }

    private static DocumentoLegal termoNovo() {
        return new DocumentoLegal(TipoDocumento.TERMO, "2.0", "texto", "0".repeat(64),
                LocalDate.now(), true, "Mudança");
    }

    private MockHttpServletResponse executar(boolean ativo, String metodo, String caminho) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(metodo, caminho);
        MockHttpServletResponse response = new MockHttpServletResponse();
        new AceitePendenteFilter(documentoLegalService, ativo).doFilter(request, response, new MockFilterChain());
        return response;
    }

    @Test
    @DisplayName("com pendência, as demais rotas respondem 428 com a lista do que falta aceitar")
    void comPendencia_bloqueiaComStatus428() throws Exception {
        when(documentoLegalService.pendentesPorUsername(EMAIL)).thenReturn(List.of(termoNovo()));

        MockHttpServletResponse response = executar(true, "GET", "/turmas/all");

        assertThat(response.getStatus()).isEqualTo(428);
        assertThat(response.getContentAsString())
                .contains("\"codigo\":\"ACEITE_PENDENTE\"")
                .contains("{\"tipo\":\"TERMO\",\"versao\":\"2.0\"}");
    }

    @Test
    @DisplayName("com pendência, continuam liberados documentos, aceites, perfil, exportação e exclusão")
    void comPendencia_liberaAsRotasDoAceite() throws Exception {
        when(documentoLegalService.pendentesPorUsername(EMAIL)).thenReturn(List.of(termoNovo()));

        assertThat(executar(true, "GET", "/documentos/termo/atual").getStatus()).isEqualTo(200);
        assertThat(executar(true, "GET", "/usuarios/me/aceites").getStatus()).isEqualTo(200);
        assertThat(executar(true, "POST", "/usuarios/me/aceites").getStatus()).isEqualTo(200);
        assertThat(executar(true, "GET", "/usuarios/me").getStatus()).isEqualTo(200);
        assertThat(executar(true, "GET", "/usuarios/me/exportar").getStatus()).isEqualTo(200);
        assertThat(executar(true, "POST", "/usuarios/me/exclusao").getStatus()).isEqualTo(200);

        // Trocar a senha pelo perfil não é liberado: primeiro a pessoa aceita
        assertThat(executar(true, "PUT", "/usuarios/me/senha").getStatus()).isEqualTo(428);
    }

    @Test
    @DisplayName("aluno que ainda não confirmou a idade recebe 428, mas pode informar a data")
    void idadePendente_bloqueiaComStatus428() throws Exception {
        Usuario contaAntiga = new Usuario();
        contaAntiga.setId(2L);
        contaAntiga.setUsuario("antigo@teste.com");
        contaAntiga.setSenha("hash");
        contaAntiga.setTipoUsuario("ALUNO");
        UserDetailsImpl usuario = new UserDetailsImpl(contaAntiga);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));
        when(documentoLegalService.pendentesPorUsername("antigo@teste.com")).thenReturn(List.of());

        MockHttpServletResponse bloqueada = executar(true, "GET", "/turmas/all");
        assertThat(bloqueada.getStatus()).isEqualTo(428);
        assertThat(bloqueada.getContentAsString()).contains("\"codigo\":\"IDADE_PENDENTE\"");

        assertThat(executar(true, "POST", "/usuarios/me/idade").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("sem pendência, nada muda")
    void semPendencia_deixaPassar() throws Exception {
        when(documentoLegalService.pendentesPorUsername(EMAIL)).thenReturn(List.of());

        assertThat(executar(true, "GET", "/turmas/all").getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("desligado (EXIGIR_ACEITE=false), não consulta nada e deixa passar")
    void desligado_naoConsulta() throws Exception {
        assertThat(executar(false, "GET", "/turmas/all").getStatus()).isEqualTo(200);
        verify(documentoLegalService, never()).pendentesPorUsername(anyString());
    }

    @Test
    @DisplayName("o token do Grafana não é uma pessoa: não passa pela conferência")
    void tokenDoGrafana_naoEConferido() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                "grafana", null, List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD"))));

        assertThat(executar(true, "GET", "/dashboard/resumo").getStatus()).isEqualTo(200);
        verify(documentoLegalService, never()).pendentesPorUsername(anyString());
    }
}

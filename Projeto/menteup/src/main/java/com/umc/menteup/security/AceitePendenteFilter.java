package com.umc.menteup.security;

import com.umc.menteup.model.DocumentoLegal;
import com.umc.menteup.service.DocumentoLegalService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

// Quem ainda não aceitou a versão atual do Termo ou da Política não usa o resto da plataforma:
// qualquer rota responde 428 (ACEITE_PENDENTE) até a pessoa aceitar
// Continuam liberados: ler os documentos, ver e registrar os aceites, ver o perfil,
// baixar os próprios dados e excluir a conta (quem não concorda pode sair)
// Ligado pela variável EXIGIR_ACEITE (desligado por padrão, para o front antigo não travar)
// Não é um @Component de propósito: só roda dentro da cadeia de segurança, depois do token
public class AceitePendenteFilter extends OncePerRequestFilter {

    private static final List<String> LIBERADAS = List.of(
            "/documentos/",
            "/usuarios/me/aceites",
            "/usuarios/me/exportar",
            "/usuarios/me/exclusao",
            "/error"
    );

    private final DocumentoLegalService documentoLegalService;
    private final boolean ativo;

    public AceitePendenteFilter(DocumentoLegalService documentoLegalService, boolean ativo) {
        this.documentoLegalService = documentoLegalService;
        this.ativo = ativo;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();

        // Só confere quem entrou com o token de login (o token do Grafana não é uma pessoa)
        if (!ativo || autenticacao == null || !(autenticacao.getPrincipal() instanceof UserDetailsImpl usuario)
                || liberada(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        List<DocumentoLegal> pendentes = documentoLegalService.pendentesPorUsername(usuario.getUsername());
        if (pendentes.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        responderAceitePendente(response, pendentes);
    }

    private static boolean liberada(HttpServletRequest request) {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }
        String caminho = request.getRequestURI().substring(request.getContextPath().length());

        // GET /usuarios/me: o front precisa do perfil para montar a tela de aceite
        if ("/usuarios/me".equals(caminho) && HttpMethod.GET.matches(request.getMethod())) {
            return true;
        }
        return LIBERADAS.stream().anyMatch(caminho::startsWith);
    }

    // Mesmo formato de erro do resto da API, com a lista do que falta aceitar
    private static void responderAceitePendente(HttpServletResponse response, List<DocumentoLegal> pendentes)
            throws IOException {
        String documentos = pendentes.stream()
                .map(d -> "{\"tipo\":\"" + d.getTipo().name() + "\",\"versao\":\"" + escapar(d.getVersao()) + "\"}")
                .collect(Collectors.joining(",", "[", "]"));

        response.setStatus(428);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"codigo\":\"ACEITE_PENDENTE\","
                        + "\"mensagem\":\"O Termo de Aceite ou a Política de Privacidade foi atualizado. "
                        + "Leia e aceite a nova versão para continuar.\","
                        + "\"documentos\":" + documentos + "}");
    }

    private static String escapar(String texto) {
        return texto.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

package com.umc.menteup.security;


import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/*

 - Vale SOMENTE para GET /dashboard/** e dá o papel DASHBOARD (só leitura de indicadores).
 -  O token vem da variável de ambiente MENTEUP_DASHBOARD_TOKEN, sem ela, este acesso fica desligado.
 */
@Component
public class DashboardTokenFilter extends OncePerRequestFilter {

    public static final String CABECALHO = "X-Dashboard-Token";

    private final byte[] tokenEsperado;

    public DashboardTokenFilter(@Value("${menteup.dashboard.token:}") String token) {
        this.tokenEsperado = token == null || token.isBlank() ? null : token.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String enviado = request.getHeader(CABECALHO);

        if (enviado != null && rotaDaDashboard(request) && tokenValido(enviado)) {
            UsernamePasswordAuthenticationToken grafana = new UsernamePasswordAuthenticationToken(
                    "grafana", null, List.of(new SimpleGrantedAuthority("ROLE_DASHBOARD")));
            SecurityContextHolder.getContext().setAuthentication(grafana);
        }

        filterChain.doFilter(request, response);
    }

    private static boolean rotaDaDashboard(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && request.getRequestURI().startsWith("/dashboard/");
    }

    private boolean tokenValido(String enviado) {
        return tokenEsperado != null
                && MessageDigest.isEqual(tokenEsperado, enviado.getBytes(StandardCharsets.UTF_8));
    }
}


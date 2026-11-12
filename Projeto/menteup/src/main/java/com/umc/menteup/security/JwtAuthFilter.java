package com.umc.menteup.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;


@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String token = extractTokenFromRequest(request);

        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            // Qualquer problema com o token é 401 (e não 500):
            // - JwtException: expirado, assinatura errada, malformado, não suportado...
            // - AuthenticationException: usuário do token não existe mais / token não confere
            // - IllegalArgumentException: token vazio ou com conteúdo inválido
            try {
                processJwtAuthentication(request, token);
            } catch (JwtException | AuthenticationException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
                responderNaoAutenticado(response);
                return;
            }
        }

        // Fora do try: um erro dentro de um controller não pode virar "token inválido"
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromRequest(HttpServletRequest request) {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ") && authHeader.length() > 7) {
            return authHeader.substring(7);
        }

        return null;
    }

    private void processJwtAuthentication(HttpServletRequest request, String token) {

        String username = jwtService.extractUsername(token);

        if (username == null || username.trim().isEmpty()) {
            throw new BadCredentialsException("Usuário não pode ser extraído do token JWT");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.validateToken(token, userDetails)) {
            throw new BadCredentialsException("Token JWT inválido ou expirado");
        }

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());

        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    // Mesmo formato de erro do resto da API (ErroResponse)
    private static void responderNaoAutenticado(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(
                "{\"codigo\":\"TOKEN_INVALIDO\","
                        + "\"mensagem\":\"Sessão inválida ou expirada. Faça o login novamente.\"}");
    }
}

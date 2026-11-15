package com.umc.menteup.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.umc.menteup.service.DocumentoLegalService;

import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/usuarios/logar",
            "/usuarios/cadastrar",
            "/usuarios/validar-2fa",
            "/usuarios/reenviar-2fa",
            "/error/**",
            "/",
            "/docs",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/usuarios/senha/esqueci",
            "/usuarios/senha/redefinir",
            "/usuarios/senha/primeiro-acesso",
    };

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Autowired
    private DashboardTokenFilter dashboardTokenFilter;

    @Autowired
    private DocumentoLegalService documentoLegalService;

    // true: quem não aceitou a versão atual do Termo ou da Política recebe 428 nas demais rotas
    // Ligue no Render (EXIGIR_ACEITE=true) só depois que o front souber mostrar a tela de aceite
    @Value("${menteup.documentos.exigir-aceite}")
    private boolean exigirAceite;

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // Endereços do front que podem chamar a API, separados por vírgula (variável CORS_ORIGENS)
    // Aceita curinga, ex.: https://menteup-*-meutime.vercel.app para as prévias do Vercel
    @Value("${menteup.cors.origens}")
    private List<String> origensPermitidas;

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(origensPermitidas);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        /// Guarda a liberação por 1 hora para o navegador não perguntar toda vez
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        // Texto do Termo e da Política: lido antes do cadastro e pelo rodapé, sem login
                        .requestMatchers(HttpMethod.GET, "/documentos/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS).permitAll()
                        .requestMatchers("/alunos/me/**").hasRole("ALUNO")
                        .requestMatchers("/logs/**").hasRole("ADMINISTRADOR")
                        // Criar usuário de qualquer tipo (inclusive administrador): só administrador
                        .requestMatchers(HttpMethod.POST, "/usuarios").hasRole("ADMINISTRADOR")
                        ///Responder pedidos de professor: só administrador
                        .requestMatchers(HttpMethod.PUT, "/usuarios/*/aprovar", "/usuarios/*/rejeitar")
                        .hasRole("ADMINISTRADOR")
                        ///Excluir usuário: só administrador
                        .requestMatchers(HttpMethod.DELETE, "/usuarios/*").hasRole("ADMINISTRADOR")
                        // Painel do sistema: todo usuário logado (o service recorta os dados por perfil).
                        // O token do Grafana não serve aqui.
                        .requestMatchers(HttpMethod.GET, "/dashboard/painel")
                        .hasAnyRole("ALUNO", "PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.GET, "/dashboard/**").hasAnyRole("ADMINISTRADOR", "DASHBOARD")
                        .requestMatchers("/dashboard/**").denyAll()
                        ///só os professores e administradores cadastram ou editam questões
                        .requestMatchers("/atividades/*/questionario").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        // Criar, editar e excluir atividade: só professor e administrador.
                        // O aluno continua podendo ver (GET) e salvar o progresso (PUT /atividades/{id}/progresso).
                        // Listagem completa traz as respostas certas: aluno usa /alunos/me/atividades
                        .requestMatchers(HttpMethod.GET, "/atividades", "/atividades/titulo/**")
                        .hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.POST, "/atividades").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.PUT, "/atividades").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .requestMatchers(HttpMethod.DELETE, "/atividades/*").hasAnyRole("PROFESSOR", "ADMINISTRADOR")
                        .anyRequest().authenticated()

                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                        "Não autorizado - Token JWT ausente ou inválido"))
                )

                .addFilterBefore(dashboardTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                // Depois do token: já sabe quem é a pessoa para conferir o aceite
                .addFilterAfter(new AceitePendenteFilter(documentoLegalService, exigirAceite),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

}
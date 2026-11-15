package com.umc.menteup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;


@Component
public class JwtService {

    // Versão da sessão gravada no token: se a conta mudar de versão, o token deixa de valer
    private static final String VERSAO_SESSAO = "sv";

    private Duration expiration = Duration.ofMinutes(60);

    private final SecretKey signingKey;

    // A chave vem da variável de ambiente JWT_SECRET (application.properties: menteup.jwt.secret).
    // Nunca escreva a chave no código: quem tiver a chave consegue gerar token de qualquer usuário.
    public JwtService(@Value("${menteup.jwt.secret}") String secret) {
        this.signingKey = criarChave(secret);
    }

    private static SecretKey criarChave(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente JWT_SECRET (chave em Base64 com pelo menos 32 bytes).");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret.trim());
        } catch (RuntimeException e) {
            throw new IllegalStateException("JWT_SECRET precisa estar em Base64.", e);
        }

        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET é curta demais: use pelo menos 32 bytes (256 bits) em Base64.");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    private SecretKey getSigningKey() {
        return signingKey;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    public Date extractExpiration(String token) {
        return extractAllClaims(token).getExpiration();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject().equals(userDetails.getUsername())
                && claims.getExpiration().after(new Date())
                && mesmaVersaoDeSessao(claims, userDetails);
    }

    // Token sem versão (gerado antes desta regra) conta como versão 0
    private static boolean mesmaVersaoDeSessao(Claims claims, UserDetails userDetails) {
        if (!(userDetails instanceof UserDetailsImpl usuario)) {
            return true;
        }
        Integer versao = claims.get(VERSAO_SESSAO, Integer.class);
        return (versao == null ? 0 : versao) == usuario.getVersaoSessao();
    }

    public String generateToken(String username) {
        return generateToken(username, 0);
    }

    public String generateToken(String username, int versaoSessao) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim(VERSAO_SESSAO, versaoSessao)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(getSigningKey())
                .compact();
    }
}

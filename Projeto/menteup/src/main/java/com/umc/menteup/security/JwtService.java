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
        return claims.getSubject().equals(userDetails.getUsername()) &&
                claims.getExpiration().after(new Date());
    }

    public String generateToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(getSigningKey())
                .compact();
    }
}

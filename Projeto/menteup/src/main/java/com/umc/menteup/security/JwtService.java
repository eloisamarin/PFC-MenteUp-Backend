package com.umc.menteup.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;


@Component
public class JwtService {

    private String secret = "46bfbfbfcf136a14d0357c1547f4cb36f9e304c2d04004581713386b658c6e1d";
    private Duration expiration = Duration.ofMinutes(60);

    private SecretKey signingKey;

    private SecretKey getSigningKey() {
        if (signingKey == null) {
            byte[] keyBytes = Decoders.BASE64.decode(secret);
            signingKey = Keys.hmacShaKeyFor(keyBytes);
        }
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

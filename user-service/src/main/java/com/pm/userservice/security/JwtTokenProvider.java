package com.pm.userservice.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(@Value("${jwt.secret}")
                            String secret,
                            @Value("${jwt.access-token-expiration}")
                            long accessTokenExpiration,
                            @Value("${jwt.refresh-token-expiration}")
                            long refreshTokenExpiration){

        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }
    public String generateAccessToken(UUID userId, String role){
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .claim("role",role)
                .expiration(new Date(now.getTime() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }
    public String generateRefreshToken(UUID userId){
        Date now  = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .signWith(key)
                .expiration(new Date(now.getTime() + refreshTokenExpiration))
                .compact();
    }
    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(
                Jwts.parser().verifyWith(key).build()
                        .parseSignedClaims(token)
                        .getPayload().getSubject());
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token)
                .getPayload().get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}

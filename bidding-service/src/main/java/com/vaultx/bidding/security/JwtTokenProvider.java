package com.vaultx.bidding.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final PublicKey publicKey;

    public JwtTokenProvider(@Value("${jwt.public-key-path:jwt-public.pem}")
                            String publicKeyPath) {
        this.publicKey = loadPublicKey(publicKeyPath);
    }

    public UUID getUserIdFromToken(String token) {
        return UUID.fromString(
                Jwts.parser().verifyWith(publicKey).build()
                        .parseSignedClaims(token)
                        .getPayload().getSubject());
    }

    public String getRoleFromToken(String token) {
        return Jwts.parser().verifyWith(publicKey).build()
                .parseSignedClaims(token)
                .getPayload().get("role", String.class);
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Date getExpirationFromToken(String token) {
        return Jwts.parser().verifyWith(publicKey).build()
                .parseSignedClaims(token)
                .getPayload().getExpiration();
    }

    private PublicKey loadPublicKey(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Key resource not found: " + path);
            }
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(der));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA public key from " + path, e);
        }
    }
}

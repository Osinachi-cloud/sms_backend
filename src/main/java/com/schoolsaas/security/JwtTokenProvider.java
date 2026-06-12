package com.schoolsaas.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String generateAccessToken(UUID userId, String email, String platformRole, UUID schoolId, Set<String> permissions) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + accessTokenExpiration);

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("type", "ACCESS")
                .issuedAt(now)
                .expiration(expiryDate);

        if (platformRole != null) {
            builder.claim("platformRole", platformRole);
        }
        if (schoolId != null) {
            builder.claim("schoolId", schoolId.toString());
        }
        if (permissions != null && !permissions.isEmpty()) {
            builder.claim("permissions", permissions);
        }

        return builder.signWith(secretKey).compact();
    }

    public String generateRefreshToken(UUID userId) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpiration);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "REFRESH")
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public UUID getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return UUID.fromString(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("email", String.class);
    }

    public String getPlatformRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("platformRole", String.class);
    }

    public UUID getSchoolIdFromToken(String token) {
        Claims claims = parseToken(token);
        String schoolId = claims.get("schoolId", String.class);
        return schoolId != null ? UUID.fromString(schoolId) : null;
    }

    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("type", String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> getPermissionsFromToken(String token) {
        Claims claims = parseToken(token);
        Object perms = claims.get("permissions");
        if (perms instanceof List<?>) {
            return new HashSet<>((List<String>) perms);
        }
        return Set.of();
    }

    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}

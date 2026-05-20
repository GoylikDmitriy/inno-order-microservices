package com.goylik.auth_service.security.jwt;

import com.goylik.auth_service.model.dto.response.TokenValidationResponse;
import com.goylik.auth_service.model.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.access-token-expiration-ms}")
    private long accessTokenExpirationMs;

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Value("${app.jwt.clock-skew-seconds:30}")
    private long clockSkewSeconds;

    private static final String ACCESS_TOKEN_TYPE_STRING = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE_STRING = "REFRESH";

    public String generateAccessToken(Long userId, Role role) {
        return buildToken(userId, role, accessTokenExpirationMs, ACCESS_TOKEN_TYPE_STRING);
    }

    public String generateRefreshToken(Long userId, Role role) {
        return buildToken(userId, role, refreshTokenExpirationMs, REFRESH_TOKEN_TYPE_STRING);
    }

    private String buildToken(Long userId, Role role, long expirationMs, String tokenType) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claims(Map.of(
                        "role", role.name(),
                        "type", tokenType
                ))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public TokenValidationResponse extractAllFromAccessToken(String token) {
        return extractAll(token, ACCESS_TOKEN_TYPE_STRING);
    }

    public TokenValidationResponse extractAllFromRefreshToken(String token) {
        return extractAll(token, REFRESH_TOKEN_TYPE_STRING);
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    public Role extractRole(String token) {
        String roleName = extractAllClaims(token).get("role", String.class);
        return Role.valueOf(roleName);
    }

    private TokenValidationResponse extractAll(String token, String type) {
        try {
            Claims claims = extractAllClaims(token);
            if (type.equals(claims.get("type", String.class))) {
                Long userId = Long.parseLong(claims.getSubject());
                Role role = Role.valueOf(claims.get("role", String.class));
                return TokenValidationResponse.success(userId, role);
            }

            return TokenValidationResponse.failure();
        } catch (Exception e) {
            return TokenValidationResponse.failure();
        }
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .clockSkewSeconds(clockSkewSeconds)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}

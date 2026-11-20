package ru.delivery.courierdelivery.security.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    public static final String TYPE_ACCESS = "access";
    public static final String TYPE_REFRESH = "refresh";

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            for (int i = 0; i < 32; i++) {
                padded[i] = bytes[i % bytes.length];
            }
            bytes = padded;
        }
        this.key = Keys.hmacShaKeyFor(bytes);
    }

    public record IssuedToken(String token, Instant expiresAt, String jti) {
    }

    public IssuedToken createAccessToken(String username, String role) {
        return create(username, role, TYPE_ACCESS, properties.getAccessTtlSeconds());
    }

    public IssuedToken createRefreshToken(String username, String role) {
        return create(username, role, TYPE_REFRESH, properties.getRefreshTtlSeconds());
    }

    private IssuedToken create(String username, String role, String type, long ttlSeconds) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        String jti = UUID.randomUUID().toString();
        String token = Jwts.builder()
                .id(jti)
                .subject(username)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(Date.from(now))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
        Instant expSecond = Instant.ofEpochSecond(exp.getEpochSecond());
        return new IssuedToken(token, expSecond, jti);
    }

    public Claims parseAndValidate(String token, String expectedType) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (claims.getExpiration() == null) {
                throw new JwtException("Отсутствует exp");
            }
            if (claims.getIssuedAt() == null) {
                throw new JwtException("Отсутствует iat");
            }
            if (claims.getSubject() == null || claims.getSubject().isBlank()) {
                throw new JwtException("Отсутствует sub");
            }
            if (claims.getId() == null || claims.getId().isBlank()) {
                throw new JwtException("Отсутствует jti");
            }
            Object type = claims.get("type");
            if (type == null || !expectedType.equals(type.toString())) {
                throw new JwtException("Неверный type токена, ожидался " + expectedType);
            }
            return claims;
        } catch (ExpiredJwtException e) {
            throw new JwtException("Токен истёк", e);
        } catch (JwtException e) {
            throw e;
        } catch (Exception e) {
            throw new JwtException("Невалидный JWT", e);
        }
    }

    public boolean isAccessToken(String token) {
        try {
            parseAndValidate(token, TYPE_ACCESS);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }
}

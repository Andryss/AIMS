package gov.mib.aims.backend.security;

import gov.mib.aims.backend.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Сервис выпуска и проверки JWT.
 */
@Service
public class JwtService {

    private final SecretKey secretKey;
    private final long expirationMs;

    /**
     * Создаёт сервис JWT из настроек приложения.
     *
     * @param jwtProperties параметры JWT
     */
    public JwtService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expirationMs = jwtProperties.getExpirationMs();
    }

    /**
     * Выпускает JWT для пользователя.
     *
     * @param login логин пользователя
     * @return токен доступа
     */
    public String generateToken(String login) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(login)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    /**
     * Извлекает логин из JWT.
     *
     * @param token токен доступа
     * @return логин из claim sub
     */
    public String extractLogin(String token) {
        return parseClaims(token).getSubject();
    }

    /**
     * Проверяет валидность JWT.
     *
     * @param token токен доступа
     * @return true, если токен валиден
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

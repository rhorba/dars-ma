package ma.darsma.backend.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import ma.darsma.backend.auth.Role;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final long accessTtlMinutes;

    public JwtService(
            @Value("${jwt.signing-key}") String signingKey,
            @Value("${jwt.access-ttl-minutes}") long accessTtlMinutes
    ) {
        this.signingKey = Keys.hmacShaKeyFor(signingKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    public long accessTtlSeconds() {
        return accessTtlMinutes * 60;
    }

    public String generateAccessToken(UUID userId, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", role.name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}

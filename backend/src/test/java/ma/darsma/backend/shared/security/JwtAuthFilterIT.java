package ma.darsma.backend.shared.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@Testcontainers
class JwtAuthFilterIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:pg16").asCompatibleSubstituteFor("postgres"))
            .withDatabaseName("dars_ma_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    // Mirrors application.yml's dev-default jwt.signing-key (tests don't override it).
    private static final SecretKey SIGNING_KEY = Keys.hmacShaKeyFor(
            "dev-only-signing-key-do-not-use-in-production-please-change-me-now".getBytes(StandardCharsets.UTF_8));

    @Test
    void expiredToken_isRejectedWith403() throws Exception {
        Instant now = Instant.now();
        String expiredToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "STUDENT")
                .issuedAt(Date.from(now.minus(20, ChronoUnit.MINUTES)))
                .expiration(Date.from(now.minus(5, ChronoUnit.MINUTES)))
                .signWith(SIGNING_KEY)
                .compact();

        // No custom AuthenticationEntryPoint is configured, so Spring Security's anonymous-auth
        // default turns "not authenticated" into an access-denied 403, not a 401 - matches every
        // other unauthenticated-access assertion in this test suite (see e.g. TutorProfileControllerIT,
        // VerificationDocumentControllerIT).
        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void tamperedToken_isRejectedWith403() throws Exception {
        String validToken = Jwts.builder()
                .subject(UUID.randomUUID().toString())
                .claim("role", "STUDENT")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plus(15, ChronoUnit.MINUTES)))
                .signWith(SIGNING_KEY)
                .compact();
        // Flip the FIRST character of the signature segment. Flipping the last character isn't safe:
        // an HMAC-SHA256 signature is 32 bytes, which base64url-encodes to 43 chars with 2 unused
        // padding bits in the final char - java.util.Base64's URL decoder (used by JJWT) doesn't
        // validate those bits, so some flips there silently decode to the same signature bytes.
        String[] parts = validToken.split("\\.");
        char[] sigChars = parts[2].toCharArray();
        sigChars[0] = sigChars[0] == 'A' ? 'B' : 'A';
        String tamperedToken = parts[0] + "." + parts[1] + "." + new String(sigChars);

        mockMvc.perform(get("/api/v1/notifications")
                        .header("Authorization", "Bearer " + tamperedToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void missingToken_isRejectedWith403() throws Exception {
        mockMvc.perform(get("/api/v1/notifications"))
                .andExpect(status().isForbidden());
    }
}

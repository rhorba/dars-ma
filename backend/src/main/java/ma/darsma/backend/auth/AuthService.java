package ma.darsma.backend.auth;

import ma.darsma.backend.auth.dto.LoginRequest;
import ma.darsma.backend.auth.dto.RegisterRequest;
import ma.darsma.backend.shared.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import static org.springframework.http.HttpStatus.*;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_BASE = Duration.ofMinutes(1);
    private static final Duration REFRESH_TTL = Duration.ofDays(7);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public void register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new ResponseStatusException(FORBIDDEN, "Admin accounts are provisioned, not self-registered");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(CONFLICT, "Email already registered");
        }
        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .fullName(request.fullName())
                .build();
        userRepository.save(user);
    }

    @Transactional
    public TokenPair login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid credentials"));

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new ResponseStatusException(TOO_MANY_REQUESTS, "Account temporarily locked, try again later");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedAttempt(user);
            throw new ResponseStatusException(UNAUTHORIZED, "Invalid credentials");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        return issueTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String presentedRawRefreshToken) {
        String presentedHash = hash(presentedRawRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(presentedHash)
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));

        if (stored.getRevokedAt() != null) {
            // Reuse of a revoked token — treat as compromise, kill every session for this user.
            refreshTokenRepository.deleteAllByUserId(stored.getUserId());
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token reuse detected, all sessions revoked");
        }
        if (!stored.isActive()) {
            throw new ResponseStatusException(UNAUTHORIZED, "Refresh token expired");
        }

        User user = userRepository.findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(UNAUTHORIZED, "Invalid refresh token"));

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        return issueTokenPair(user);
    }

    private void registerFailedAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            long backoffMultiplier = 1L << (attempts - MAX_FAILED_ATTEMPTS);
            user.setLockedUntil(Instant.now().plus(LOCKOUT_BASE.multipliedBy(backoffMultiplier)));
        }
        userRepository.save(user);
    }

    private TokenPair issueTokenPair(User user) {
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole());

        String rawRefreshToken = generateRawToken();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(Instant.now().plus(REFRESH_TTL.toDays(), ChronoUnit.DAYS))
                .build();
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(accessToken, jwtService.accessTtlSeconds(), rawRefreshToken);
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}

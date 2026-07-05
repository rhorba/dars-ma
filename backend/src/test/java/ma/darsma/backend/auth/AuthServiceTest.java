package ma.darsma.backend.auth;

import ma.darsma.backend.auth.dto.LoginRequest;
import ma.darsma.backend.auth.dto.RegisterRequest;
import ma.darsma.backend.shared.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void register_savesStudentWithHashedPassword() {
        RegisterRequest request = new RegisterRequest("yasmine@example.com", "supersecret1", Role.STUDENT, "Yasmine");
        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(passwordEncoder.encode(request.password())).thenReturn("hashed");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getRole()).isEqualTo(Role.STUDENT);
    }

    @Test
    void register_rejectsAdminSelfRegistration() {
        RegisterRequest request = new RegisterRequest("admin@example.com", "supersecret1", Role.ADMIN, "Admin");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Admin accounts are provisioned");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void register_rejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest("karim@example.com", "supersecret1", Role.TUTOR, "Karim");
        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void login_succeedsWithValidCredentialsAndIssuesTokenPair() {
        User user = existingUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("supersecret1", user.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(user.getId(), user.getRole())).thenReturn("access-token");
        when(jwtService.accessTtlSeconds()).thenReturn(900L);

        TokenPair tokens = authService.login(new LoginRequest(user.getEmail(), "supersecret1"));

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.rawRefreshToken()).isNotBlank();
        assertThat(user.getFailedLoginAttempts()).isZero();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_wrongPasswordIncrementsFailedAttempts() {
        User user = existingUser();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(1);
    }

    @Test
    void login_locksAccountAfterFiveFailedAttempts() {
        User user = existingUser();
        user.setFailedLoginAttempts(4);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "wrong")))
                .isInstanceOf(ResponseStatusException.class);

        assertThat(user.getFailedLoginAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isAfter(Instant.now());
    }

    @Test
    void login_rejectedWhileAccountIsLocked() {
        User user = existingUser();
        user.setLockedUntil(Instant.now().plusSeconds(60));
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest(user.getEmail(), "supersecret1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("locked");
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void refresh_reusingARevokedTokenRevokesAllSessionsForThatUser() {
        UUID userId = UUID.randomUUID();
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .tokenHash("irrelevant-because-mocked-lookup")
                .expiresAt(Instant.now().plusSeconds(3600))
                .revokedAt(Instant.now().minusSeconds(10))
                .build();
        when(refreshTokenRepository.findByTokenHash(any())).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> authService.refresh("some-raw-token"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("reuse detected");

        verify(refreshTokenRepository).deleteAllByUserId(userId);
    }

    private User existingUser() {
        return User.builder()
                .id(UUID.randomUUID())
                .email("karim@example.com")
                .passwordHash("hashed")
                .role(Role.TUTOR)
                .fullName("Karim")
                .failedLoginAttempts(0)
                .build();
    }
}

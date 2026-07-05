package ma.darsma.backend.auth;

import jakarta.validation.Valid;
import ma.darsma.backend.auth.dto.AuthResponse;
import ma.darsma.backend.auth.dto.LoginRequest;
import ma.darsma.backend.auth.dto.RegisterRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refresh_token";

    private final AuthService authService;
    private final boolean cookieSecure;

    public AuthController(AuthService authService,
                           @Value("${app.cookie-secure:true}") boolean cookieSecure) {
        this.authService = authService;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokens = authService.login(request);
        return respondWithTokens(tokens);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@CookieValue(REFRESH_COOKIE_NAME) String refreshToken) {
        TokenPair tokens = authService.refresh(refreshToken);
        return respondWithTokens(tokens);
    }

    private ResponseEntity<AuthResponse> respondWithTokens(TokenPair tokens) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, tokens.rawRefreshToken())
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Strict")
                .path("/api/v1/auth")
                .maxAge(java.time.Duration.ofDays(7))
                .build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(tokens.accessToken(), tokens.accessTtlSeconds()));
    }
}

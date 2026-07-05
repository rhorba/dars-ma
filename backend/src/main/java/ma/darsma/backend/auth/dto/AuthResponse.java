package ma.darsma.backend.auth.dto;

public record AuthResponse(
        String accessToken,
        long accessTokenExpiresInSeconds
) {
}

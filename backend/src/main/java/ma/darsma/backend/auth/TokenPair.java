package ma.darsma.backend.auth;

public record TokenPair(String accessToken, long accessTtlSeconds, String rawRefreshToken) {
}

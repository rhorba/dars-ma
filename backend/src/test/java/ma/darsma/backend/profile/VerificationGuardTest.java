package ma.darsma.backend.profile;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificationGuardTest {

    private final VerificationGuard guard = new VerificationGuard();

    private TutorProfile profileWith(VerificationStatus status) {
        return TutorProfile.builder()
                .userId(UUID.randomUUID())
                .subjects(new String[]{"Math"})
                .hourlyRateMad(new BigDecimal("100.00"))
                .verificationStatus(status)
                .build();
    }

    @Test
    void assertVerified_passesForVerifiedTutor() {
        assertThatCode(() -> guard.assertVerified(profileWith(VerificationStatus.VERIFIED)))
                .doesNotThrowAnyException();
    }

    @Test
    void assertVerified_rejectsPendingTutor() {
        assertThatThrownBy(() -> guard.assertVerified(profileWith(VerificationStatus.PENDING)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void assertVerified_rejectsRejectedTutor() {
        assertThatThrownBy(() -> guard.assertVerified(profileWith(VerificationStatus.REJECTED)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("REJECTED");
    }
}

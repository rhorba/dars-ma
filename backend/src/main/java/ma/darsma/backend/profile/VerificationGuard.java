package ma.darsma.backend.profile;

import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.FORBIDDEN;

/**
 * Reused by booking-acceptance (Epic 4) to enforce that only verified tutors can accept a match.
 */
@Component
public class VerificationGuard {

    public void assertVerified(TutorProfile tutorProfile) {
        if (tutorProfile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            throw new ResponseStatusException(FORBIDDEN,
                    "Tutor verification is " + tutorProfile.getVerificationStatus() + " — must be VERIFIED to accept bookings");
        }
    }
}

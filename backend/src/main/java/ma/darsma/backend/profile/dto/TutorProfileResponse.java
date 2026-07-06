package ma.darsma.backend.profile.dto;

import ma.darsma.backend.profile.TutorProfile;
import ma.darsma.backend.profile.VerificationStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record TutorProfileResponse(
        UUID userId,
        String bio,
        String[] subjects,
        BigDecimal hourlyRateMad,
        VerificationStatus verificationStatus,
        BigDecimal avgRating
) {
    public static TutorProfileResponse from(TutorProfile profile) {
        return new TutorProfileResponse(
                profile.getUserId(),
                profile.getBio(),
                profile.getSubjects(),
                profile.getHourlyRateMad(),
                profile.getVerificationStatus(),
                profile.getAvgRating());
    }
}

package ma.darsma.backend.profile;

import ma.darsma.backend.matching.EmbeddingService;
import ma.darsma.backend.profile.dto.TutorProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TutorProfileServiceTest {

    private TutorProfileRepository tutorProfileRepository;
    private EmbeddingService embeddingService;
    private TutorProfileService tutorProfileService;

    @BeforeEach
    void setUp() {
        tutorProfileRepository = mock(TutorProfileRepository.class);
        embeddingService = mock(EmbeddingService.class);
        tutorProfileService = new TutorProfileService(tutorProfileRepository, embeddingService);
    }

    @Test
    void upsert_createsNewProfileWhenNoneExists() {
        UUID tutorId = UUID.randomUUID();
        TutorProfileRequest request = new TutorProfileRequest("Math tutor", new String[]{"Math"}, new BigDecimal("150.00"));
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.empty());
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        TutorProfile result = tutorProfileService.upsert(tutorId, request);

        assertThat(result.getUserId()).isEqualTo(tutorId);
        assertThat(result.getBio()).isEqualTo("Math tutor");
        assertThat(result.getSubjects()).containsExactly("Math");
        assertThat(result.getHourlyRateMad()).isEqualByComparingTo("150.00");
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.PENDING);
        verify(embeddingService).embedTutorProfile(result);
    }

    @Test
    void upsert_updatesExistingProfileWithoutResettingVerificationStatus() {
        UUID tutorId = UUID.randomUUID();
        TutorProfile existing = TutorProfile.builder()
                .userId(tutorId)
                .bio("Old bio")
                .subjects(new String[]{"Physics"})
                .hourlyRateMad(new BigDecimal("100.00"))
                .verificationStatus(VerificationStatus.VERIFIED)
                .build();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(existing));
        when(tutorProfileRepository.save(any(TutorProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        TutorProfileRequest request = new TutorProfileRequest("New bio", new String[]{"Chemistry"}, new BigDecimal("200.00"));
        TutorProfile result = tutorProfileService.upsert(tutorId, request);

        assertThat(result.getBio()).isEqualTo("New bio");
        assertThat(result.getSubjects()).containsExactly("Chemistry");
        assertThat(result.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void getByUserId_throwsNotFoundWhenProfileMissing() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tutorProfileService.getByUserId(tutorId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}

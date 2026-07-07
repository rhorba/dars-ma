package ma.darsma.backend.matching;

import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.profile.TutorProfile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmbeddingServiceTest {

    private EmbeddingProvider embeddingProvider;
    private TutorEmbeddingRepository tutorEmbeddingRepository;
    private GigEmbeddingRepository gigEmbeddingRepository;
    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingProvider = mock(EmbeddingProvider.class);
        tutorEmbeddingRepository = mock(TutorEmbeddingRepository.class);
        gigEmbeddingRepository = mock(GigEmbeddingRepository.class);
        embeddingService = new EmbeddingService(embeddingProvider, tutorEmbeddingRepository, gigEmbeddingRepository);
    }

    @Test
    void embedTutorProfile_createsEmbeddingWhenNoneExists() {
        UUID tutorId = UUID.randomUUID();
        TutorProfile profile = TutorProfile.builder()
                .userId(tutorId)
                .bio("Experienced tutor")
                .subjects(new String[]{"Math", "Physics"})
                .hourlyRateMad(new BigDecimal("100.00"))
                .build();
        float[] vector = new float[]{0.1f, 0.2f};
        when(embeddingProvider.embed("Subjects: Math, Physics. Bio: Experienced tutor")).thenReturn(vector);
        when(tutorEmbeddingRepository.findById(tutorId)).thenReturn(Optional.empty());

        embeddingService.embedTutorProfile(profile);

        verify(tutorEmbeddingRepository).save(argThat(e -> e.getTutorUserId().equals(tutorId) && e.getEmbedding() == vector));
    }

    @Test
    void embedTutorProfile_updatesExistingEmbedding() {
        UUID tutorId = UUID.randomUUID();
        TutorProfile profile = TutorProfile.builder()
                .userId(tutorId)
                .subjects(new String[]{"Chemistry"})
                .hourlyRateMad(new BigDecimal("100.00"))
                .build();
        TutorEmbedding existing = new TutorEmbedding(tutorId, new float[]{0.9f});
        float[] newVector = new float[]{0.3f};
        when(embeddingProvider.embed(any())).thenReturn(newVector);
        when(tutorEmbeddingRepository.findById(tutorId)).thenReturn(Optional.of(existing));

        embeddingService.embedTutorProfile(profile);

        assertThat(existing.getEmbedding()).isEqualTo(newVector);
        verify(tutorEmbeddingRepository).save(existing);
    }

    @Test
    void embedGigRequest_createsEmbeddingWhenNoneExists() {
        UUID gigId = UUID.randomUUID();
        GigRequest gigRequest = mock(GigRequest.class);
        when(gigRequest.getId()).thenReturn(gigId);
        when(gigRequest.getSubject()).thenReturn("Math");
        when(gigRequest.getLevel()).thenReturn("High School");
        when(gigRequest.getDescription()).thenReturn("Need help with calculus");
        float[] vector = new float[]{0.4f};
        when(embeddingProvider.embed("Subject: Math. Level: High School. Need help with calculus")).thenReturn(vector);
        when(gigEmbeddingRepository.findById(gigId)).thenReturn(Optional.empty());

        embeddingService.embedGigRequest(gigRequest);

        verify(gigEmbeddingRepository).save(argThat(e -> e.getGigRequestId().equals(gigId) && e.getEmbedding() == vector));
    }
}

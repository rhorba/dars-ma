package ma.darsma.backend.matching;

import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.profile.TutorProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MatchingServiceTest {

    private GigEmbeddingRepository gigEmbeddingRepository;
    private TutorEmbeddingRepository tutorEmbeddingRepository;
    private TutorProfileRepository tutorProfileRepository;
    private MatchSuggestionRepository matchSuggestionRepository;
    private MatchingService matchingService;

    @BeforeEach
    void setUp() {
        gigEmbeddingRepository = mock(GigEmbeddingRepository.class);
        tutorEmbeddingRepository = mock(TutorEmbeddingRepository.class);
        tutorProfileRepository = mock(TutorProfileRepository.class);
        matchSuggestionRepository = mock(MatchSuggestionRepository.class);
        matchingService = new MatchingService(gigEmbeddingRepository, tutorEmbeddingRepository, tutorProfileRepository, matchSuggestionRepository);
    }

    private GigRequest gigRequest(UUID id, String subject) {
        GigRequest gigRequest = GigRequest.builder().studentUserId(UUID.randomUUID()).subject(subject).level("HS").description("desc").build();
        setId(gigRequest, id);
        return gigRequest;
    }

    private void setId(GigRequest gigRequest, UUID id) {
        try {
            var field = GigRequest.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(gigRequest, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void generateMatches_thinPool_usesSubjectFallback() {
        UUID gigId = UUID.randomUUID();
        GigRequest gigRequest = gigRequest(gigId, "Math");
        UUID tutorId = UUID.randomUUID();
        when(tutorEmbeddingRepository.countVerifiedWithEmbedding()).thenReturn((long) (MatchingService.THIN_POOL_THRESHOLD - 1));
        when(tutorProfileRepository.findVerifiedBySubjectFallback("Math", MatchingService.TOP_N)).thenReturn(List.of(tutorId));

        matchingService.generateMatches(gigRequest);

        verify(matchSuggestionRepository).deleteByGigRequestId(gigId);
        verify(tutorEmbeddingRepository, never()).findTopMatches(any(), anyInt());
        ArgumentCaptor<List<MatchSuggestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchSuggestionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTutorUserId()).isEqualTo(tutorId);
        assertThat(captor.getValue().get(0).getGigRequestId()).isEqualTo(gigId);
    }

    @Test
    void generateMatches_healthyPool_usesVectorSearch() {
        UUID gigId = UUID.randomUUID();
        GigRequest gigRequest = gigRequest(gigId, "Math");
        UUID tutorId = UUID.randomUUID();
        float[] vector = new float[EmbeddingProvider.DIMENSIONS];
        when(tutorEmbeddingRepository.countVerifiedWithEmbedding()).thenReturn((long) MatchingService.THIN_POOL_THRESHOLD);
        when(gigEmbeddingRepository.findById(gigId)).thenReturn(Optional.of(new GigEmbedding(gigId, vector)));
        TutorMatchRow row = mock(TutorMatchRow.class);
        when(row.getTutorUserId()).thenReturn(tutorId);
        when(row.getScore()).thenReturn(new BigDecimal("0.9123456"));
        when(tutorEmbeddingRepository.findTopMatches(anyString(), eq(MatchingService.TOP_N))).thenReturn(List.of(row));

        matchingService.generateMatches(gigRequest);

        verify(tutorProfileRepository, never()).findVerifiedBySubjectFallback(any(), anyInt());
        ArgumentCaptor<List<MatchSuggestion>> captor = ArgumentCaptor.forClass(List.class);
        verify(matchSuggestionRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(1);
        assertThat(captor.getValue().get(0).getTutorUserId()).isEqualTo(tutorId);
        assertThat(captor.getValue().get(0).getSimilarityScore()).isEqualByComparingTo("0.9123");
    }

    @Test
    void generateMatches_healthyPoolButNoGigEmbedding_throws() {
        UUID gigId = UUID.randomUUID();
        GigRequest gigRequest = gigRequest(gigId, "Math");
        when(tutorEmbeddingRepository.countVerifiedWithEmbedding()).thenReturn((long) MatchingService.THIN_POOL_THRESHOLD);
        when(gigEmbeddingRepository.findById(gigId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> matchingService.generateMatches(gigRequest))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getMatches_delegatesToRepositoryOrderedByScore() {
        UUID gigId = UUID.randomUUID();
        GigRequest gigRequest = gigRequest(gigId, "Math");
        MatchSuggestion suggestion = new MatchSuggestion(gigId, UUID.randomUUID(), new BigDecimal("0.5000"));
        when(matchSuggestionRepository.findByGigRequestIdOrderBySimilarityScoreDesc(gigId)).thenReturn(List.of(suggestion));

        List<MatchSuggestion> result = matchingService.getMatches(gigRequest);

        assertThat(result).containsExactly(suggestion);
    }
}

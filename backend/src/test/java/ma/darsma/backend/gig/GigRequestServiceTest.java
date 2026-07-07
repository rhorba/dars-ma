package ma.darsma.backend.gig;

import ma.darsma.backend.gig.dto.GigRequestCreateRequest;
import ma.darsma.backend.matching.EmbeddingService;
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

class GigRequestServiceTest {

    private GigRequestRepository gigRequestRepository;
    private EmbeddingService embeddingService;
    private GigRequestService gigRequestService;

    @BeforeEach
    void setUp() {
        gigRequestRepository = mock(GigRequestRepository.class);
        embeddingService = mock(EmbeddingService.class);
        gigRequestService = new GigRequestService(gigRequestRepository, embeddingService);
    }

    @Test
    void create_savesGigRequestWithOpenStatus() {
        UUID studentId = UUID.randomUUID();
        GigRequestCreateRequest request = new GigRequestCreateRequest(
                "Math", "High School", "Need help with calculus", new BigDecimal("100.00"), new BigDecimal("200.00"));
        when(gigRequestRepository.save(any(GigRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        GigRequest result = gigRequestService.create(studentId, request);

        assertThat(result.getStudentUserId()).isEqualTo(studentId);
        assertThat(result.getSubject()).isEqualTo("Math");
        assertThat(result.getStatus()).isEqualTo(GigStatus.OPEN);
        verify(embeddingService).embedGigRequest(result);
    }

    @Test
    void create_rejectsBudgetMinGreaterThanMax() {
        UUID studentId = UUID.randomUUID();
        GigRequestCreateRequest request = new GigRequestCreateRequest(
                "Math", "High School", "Need help with calculus", new BigDecimal("300.00"), new BigDecimal("200.00"));

        assertThatThrownBy(() -> gigRequestService.create(studentId, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("budgetMinMad");
        verify(gigRequestRepository, never()).save(any());
    }

    @Test
    void getForOwner_throwsNotFoundWhenMissing() {
        UUID gigId = UUID.randomUUID();
        when(gigRequestRepository.findById(gigId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gigRequestService.getForOwner(gigId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void getForOwner_throwsForbiddenWhenRequesterIsNotOwner() {
        UUID gigId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        GigRequest gigRequest = GigRequest.builder().studentUserId(ownerId).subject("Math").level("HS").description("desc").build();
        when(gigRequestRepository.findById(gigId)).thenReturn(Optional.of(gigRequest));

        assertThatThrownBy(() -> gigRequestService.getForOwner(gigId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("owner");
    }

    @Test
    void getForOwner_returnsGigWhenRequesterIsOwner() {
        UUID gigId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        GigRequest gigRequest = GigRequest.builder().studentUserId(ownerId).subject("Math").level("HS").description("desc").build();
        when(gigRequestRepository.findById(gigId)).thenReturn(Optional.of(gigRequest));

        GigRequest result = gigRequestService.getForOwner(gigId, ownerId);

        assertThat(result.getSubject()).isEqualTo("Math");
    }
}

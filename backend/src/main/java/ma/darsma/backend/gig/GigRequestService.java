package ma.darsma.backend.gig;

import ma.darsma.backend.gig.dto.GigRequestCreateRequest;
import ma.darsma.backend.matching.EmbeddingService;
import ma.darsma.backend.matching.MatchingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class GigRequestService {

    private final GigRequestRepository gigRequestRepository;
    private final EmbeddingService embeddingService;
    private final MatchingService matchingService;

    public GigRequestService(GigRequestRepository gigRequestRepository, EmbeddingService embeddingService, MatchingService matchingService) {
        this.gigRequestRepository = gigRequestRepository;
        this.embeddingService = embeddingService;
        this.matchingService = matchingService;
    }

    @Transactional
    public GigRequest create(UUID studentUserId, GigRequestCreateRequest request) {
        if (request.budgetMinMad() != null && request.budgetMaxMad() != null
                && request.budgetMinMad().compareTo(request.budgetMaxMad()) > 0) {
            throw new ResponseStatusException(BAD_REQUEST, "budgetMinMad must not exceed budgetMaxMad");
        }
        GigRequest gigRequest = GigRequest.builder()
                .studentUserId(studentUserId)
                .subject(request.subject())
                .level(request.level())
                .description(request.description())
                .budgetMinMad(request.budgetMinMad())
                .budgetMaxMad(request.budgetMaxMad())
                .build();
        GigRequest saved = gigRequestRepository.save(gigRequest);
        embeddingService.embedGigRequest(saved);
        matchingService.generateMatches(saved);
        return saved;
    }

    public GigRequest getForOwner(UUID gigId, UUID requesterId) {
        GigRequest gigRequest = gigRequestRepository.findById(gigId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Gig request not found"));
        if (!gigRequest.getStudentUserId().equals(requesterId)) {
            throw new ResponseStatusException(FORBIDDEN, "Not the owner of this gig request");
        }
        return gigRequest;
    }
}

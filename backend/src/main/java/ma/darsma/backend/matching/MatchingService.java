package ma.darsma.backend.matching;

import com.pgvector.PGvector;
import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.profile.TutorProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
public class MatchingService {

    static final int TOP_N = 10;

    // Below this many verified+embedded tutors, cosine similarity ranking is too noisy to be useful (PRD risk mitigation).
    static final int THIN_POOL_THRESHOLD = 5;

    private final GigEmbeddingRepository gigEmbeddingRepository;
    private final TutorEmbeddingRepository tutorEmbeddingRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final MatchSuggestionRepository matchSuggestionRepository;

    public MatchingService(
            GigEmbeddingRepository gigEmbeddingRepository,
            TutorEmbeddingRepository tutorEmbeddingRepository,
            TutorProfileRepository tutorProfileRepository,
            MatchSuggestionRepository matchSuggestionRepository) {
        this.gigEmbeddingRepository = gigEmbeddingRepository;
        this.tutorEmbeddingRepository = tutorEmbeddingRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.matchSuggestionRepository = matchSuggestionRepository;
    }

    @Transactional
    public void generateMatches(GigRequest gigRequest) {
        matchSuggestionRepository.deleteByGigRequestId(gigRequest.getId());

        List<MatchSuggestion> suggestions = tutorEmbeddingRepository.countVerifiedWithEmbedding() >= THIN_POOL_THRESHOLD
                ? vectorMatches(gigRequest)
                : fallbackMatches(gigRequest);

        matchSuggestionRepository.saveAll(suggestions);
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestion> getMatches(GigRequest gigRequest) {
        return matchSuggestionRepository.findByGigRequestIdOrderBySimilarityScoreDesc(gigRequest.getId());
    }

    private List<MatchSuggestion> vectorMatches(GigRequest gigRequest) {
        GigEmbedding gigEmbedding = gigEmbeddingRepository.findById(gigRequest.getId())
                .orElseThrow(() -> new IllegalStateException("Gig embedding missing for gig " + gigRequest.getId()));
        String vectorText = new PGvector(gigEmbedding.getEmbedding()).toString();
        return tutorEmbeddingRepository.findTopMatches(vectorText, TOP_N).stream()
                .map(row -> new MatchSuggestion(
                        gigRequest.getId(),
                        row.getTutorUserId(),
                        row.getScore().setScale(4, RoundingMode.HALF_UP)))
                .toList();
    }

    private List<MatchSuggestion> fallbackMatches(GigRequest gigRequest) {
        List<UUID> tutorUserIds = tutorProfileRepository.findVerifiedBySubjectFallback(gigRequest.getSubject(), TOP_N);
        return tutorUserIds.stream()
                .map(tutorUserId -> new MatchSuggestion(gigRequest.getId(), tutorUserId, BigDecimal.ZERO.setScale(4)))
                .toList();
    }
}

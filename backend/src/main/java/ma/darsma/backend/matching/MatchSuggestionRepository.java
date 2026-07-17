package ma.darsma.backend.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MatchSuggestionRepository extends JpaRepository<MatchSuggestion, UUID> {

    List<MatchSuggestion> findByGigRequestIdOrderBySimilarityScoreDesc(UUID gigRequestId);

    void deleteByGigRequestId(UUID gigRequestId);

    @Query("select count(distinct m.gigRequestId) from MatchSuggestion m")
    long countDistinctGigRequestId();
}

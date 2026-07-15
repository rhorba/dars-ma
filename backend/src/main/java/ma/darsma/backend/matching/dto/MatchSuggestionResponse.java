package ma.darsma.backend.matching.dto;

import ma.darsma.backend.matching.MatchSuggestion;

import java.math.BigDecimal;
import java.util.UUID;

public record MatchSuggestionResponse(
        UUID tutorUserId,
        BigDecimal similarityScore
) {
    public static MatchSuggestionResponse from(MatchSuggestion suggestion) {
        return new MatchSuggestionResponse(suggestion.getTutorUserId(), suggestion.getSimilarityScore());
    }
}

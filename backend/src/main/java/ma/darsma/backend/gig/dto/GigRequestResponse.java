package ma.darsma.backend.gig.dto;

import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.gig.GigStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record GigRequestResponse(
        UUID id,
        UUID studentUserId,
        String subject,
        String level,
        String description,
        BigDecimal budgetMinMad,
        BigDecimal budgetMaxMad,
        GigStatus status,
        Instant createdAt
) {
    public static GigRequestResponse from(GigRequest gigRequest) {
        return new GigRequestResponse(
                gigRequest.getId(),
                gigRequest.getStudentUserId(),
                gigRequest.getSubject(),
                gigRequest.getLevel(),
                gigRequest.getDescription(),
                gigRequest.getBudgetMinMad(),
                gigRequest.getBudgetMaxMad(),
                gigRequest.getStatus(),
                gigRequest.getCreatedAt());
    }
}

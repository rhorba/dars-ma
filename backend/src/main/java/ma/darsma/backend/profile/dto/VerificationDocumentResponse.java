package ma.darsma.backend.profile.dto;

import ma.darsma.backend.profile.DocType;
import ma.darsma.backend.profile.VerificationDocument;

import java.time.Instant;
import java.util.UUID;

public record VerificationDocumentResponse(
        UUID id,
        DocType docType,
        String originalFilename,
        Instant reviewedAt,
        Instant createdAt
) {
    public static VerificationDocumentResponse from(VerificationDocument document) {
        return new VerificationDocumentResponse(
                document.getId(),
                document.getDocType(),
                document.getOriginalFilename(),
                document.getReviewedAt(),
                document.getCreatedAt());
    }
}

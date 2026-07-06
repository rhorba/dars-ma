package ma.darsma.backend.admin.dto;

import ma.darsma.backend.profile.DocType;
import ma.darsma.backend.profile.VerificationDocument;

import java.time.Instant;
import java.util.UUID;

public record VerificationQueueItemResponse(
        UUID documentId,
        UUID tutorUserId,
        String tutorFullName,
        DocType docType,
        String originalFilename,
        Instant submittedAt
) {
    public static VerificationQueueItemResponse from(VerificationDocument document, String tutorFullName) {
        return new VerificationQueueItemResponse(
                document.getId(),
                document.getTutorUserId(),
                tutorFullName,
                document.getDocType(),
                document.getOriginalFilename(),
                document.getCreatedAt());
    }
}

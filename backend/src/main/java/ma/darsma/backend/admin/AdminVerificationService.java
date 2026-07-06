package ma.darsma.backend.admin;

import ma.darsma.backend.auth.User;
import ma.darsma.backend.auth.UserRepository;
import ma.darsma.backend.notification.NotificationService;
import ma.darsma.backend.profile.TutorProfile;
import ma.darsma.backend.profile.TutorProfileRepository;
import ma.darsma.backend.profile.VerificationDocument;
import ma.darsma.backend.profile.VerificationDocumentRepository;
import ma.darsma.backend.profile.VerificationStatus;
import ma.darsma.backend.shared.audit.AuditLogService;
import ma.darsma.backend.shared.security.DocumentEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class AdminVerificationService {

    private final VerificationDocumentRepository verificationDocumentRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final DocumentEncryptionService documentEncryptionService;

    public AdminVerificationService(VerificationDocumentRepository verificationDocumentRepository,
                                     TutorProfileRepository tutorProfileRepository,
                                     UserRepository userRepository,
                                     NotificationService notificationService,
                                     AuditLogService auditLogService,
                                     DocumentEncryptionService documentEncryptionService) {
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.documentEncryptionService = documentEncryptionService;
    }

    public List<VerificationDocument> pendingQueue() {
        return verificationDocumentRepository.findByReviewedAtIsNull();
    }

    public String tutorFullName(UUID tutorUserId) {
        return userRepository.findById(tutorUserId).map(User::getFullName).orElse("Unknown");
    }

    public VerificationDocument documentMetadata(UUID documentId) {
        return verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification document not found"));
    }

    public byte[] decryptedContent(UUID documentId) {
        return documentEncryptionService.decrypt(documentMetadata(documentId).getEncryptedBlob());
    }

    @Transactional
    public void approve(UUID documentId, UUID adminId) {
        VerificationDocument document = documentFor(documentId);
        document.setReviewedBy(adminId);
        document.setReviewedAt(Instant.now());
        verificationDocumentRepository.save(document);

        TutorProfile profile = tutorProfileRepository.findById(document.getTutorUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tutor profile not found"));
        profile.setVerificationStatus(VerificationStatus.VERIFIED);
        tutorProfileRepository.save(profile);

        notificationService.create(document.getTutorUserId(), "VERIFICATION_APPROVED",
                Map.of("documentId", documentId.toString()));
        auditLogService.record(adminId, "VERIFICATION_APPROVED", "verification_documents", documentId,
                Map.of("tutorUserId", document.getTutorUserId().toString()));
    }

    @Transactional
    public void reject(UUID documentId, UUID adminId, String reason) {
        VerificationDocument document = documentFor(documentId);
        document.setReviewedBy(adminId);
        document.setReviewedAt(Instant.now());
        verificationDocumentRepository.save(document);

        TutorProfile profile = tutorProfileRepository.findById(document.getTutorUserId())
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tutor profile not found"));
        profile.setVerificationStatus(VerificationStatus.REJECTED);
        tutorProfileRepository.save(profile);

        notificationService.create(document.getTutorUserId(), "VERIFICATION_REJECTED",
                Map.of("documentId", documentId.toString(), "reason", reason));
        auditLogService.record(adminId, "VERIFICATION_REJECTED", "verification_documents", documentId,
                Map.of("tutorUserId", document.getTutorUserId().toString(), "reason", reason));
    }

    private VerificationDocument documentFor(UUID documentId) {
        VerificationDocument document = verificationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Verification document not found"));
        if (document.getReviewedAt() != null) {
            throw new ResponseStatusException(CONFLICT, "Document has already been reviewed");
        }
        return document;
    }
}

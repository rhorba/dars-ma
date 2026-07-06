package ma.darsma.backend.admin;

import ma.darsma.backend.auth.Role;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminVerificationServiceTest {

    private VerificationDocumentRepository verificationDocumentRepository;
    private TutorProfileRepository tutorProfileRepository;
    private UserRepository userRepository;
    private NotificationService notificationService;
    private AuditLogService auditLogService;
    private DocumentEncryptionService documentEncryptionService;
    private AdminVerificationService service;

    @BeforeEach
    void setUp() {
        verificationDocumentRepository = mock(VerificationDocumentRepository.class);
        tutorProfileRepository = mock(TutorProfileRepository.class);
        userRepository = mock(UserRepository.class);
        notificationService = mock(NotificationService.class);
        auditLogService = mock(AuditLogService.class);
        documentEncryptionService = mock(DocumentEncryptionService.class);
        service = new AdminVerificationService(verificationDocumentRepository, tutorProfileRepository,
                userRepository, notificationService, auditLogService, documentEncryptionService);
    }

    private VerificationDocument unreviewedDocument(UUID docId, UUID tutorId) {
        return VerificationDocument.builder()
                .id(docId)
                .tutorUserId(tutorId)
                .docType(ma.darsma.backend.profile.DocType.DIPLOMA)
                .encryptedBlob(new byte[]{1})
                .mimeType("application/pdf")
                .build();
    }

    private TutorProfile pendingProfile(UUID tutorId) {
        return TutorProfile.builder()
                .userId(tutorId)
                .subjects(new String[]{"Math"})
                .hourlyRateMad(new BigDecimal("100.00"))
                .verificationStatus(VerificationStatus.PENDING)
                .build();
    }

    @Test
    void approve_setsVerifiedAndWritesNotificationAndAuditLog() {
        UUID docId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        VerificationDocument doc = unreviewedDocument(docId, tutorId);
        TutorProfile profile = pendingProfile(tutorId);
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(profile));

        service.approve(docId, adminId);

        assertThat(doc.getReviewedBy()).isEqualTo(adminId);
        assertThat(doc.getReviewedAt()).isNotNull();
        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(notificationService).create(eq(tutorId), eq("VERIFICATION_APPROVED"), any());
        verify(auditLogService).record(eq(adminId), eq("VERIFICATION_APPROVED"), eq("verification_documents"), eq(docId), any());
    }

    @Test
    void reject_setsRejectedAndIncludesReasonInNotificationAndAudit() {
        UUID docId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        VerificationDocument doc = unreviewedDocument(docId, tutorId);
        TutorProfile profile = pendingProfile(tutorId);
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(profile));

        service.reject(docId, adminId, "Diploma image is unreadable");

        assertThat(profile.getVerificationStatus()).isEqualTo(VerificationStatus.REJECTED);
        verify(notificationService).create(eq(tutorId), eq("VERIFICATION_REJECTED"),
                argThat(payload -> "Diploma image is unreadable".equals(payload.get("reason"))));
        verify(auditLogService).record(eq(adminId), eq("VERIFICATION_REJECTED"), eq("verification_documents"), eq(docId),
                argThat(metadata -> "Diploma image is unreadable".equals(metadata.get("reason"))));
    }

    @Test
    void approve_rejectsAlreadyReviewedDocument() {
        UUID docId = UUID.randomUUID();
        UUID tutorId = UUID.randomUUID();
        VerificationDocument doc = unreviewedDocument(docId, tutorId);
        doc.setReviewedAt(Instant.now());
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> service.approve(docId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("already been reviewed");
        verifyNoInteractions(notificationService);
    }

    @Test
    void approve_throwsNotFoundForUnknownDocument() {
        UUID docId = UUID.randomUUID();
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(docId, UUID.randomUUID()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void tutorFullName_returnsUnknownWhenUserMissing() {
        UUID tutorId = UUID.randomUUID();
        when(userRepository.findById(tutorId)).thenReturn(Optional.empty());

        assertThat(service.tutorFullName(tutorId)).isEqualTo("Unknown");
    }

    @Test
    void tutorFullName_returnsNameWhenUserExists() {
        UUID tutorId = UUID.randomUUID();
        User user = User.builder().id(tutorId).email("a@b.com").passwordHash("h").role(Role.TUTOR).fullName("Karim").build();
        when(userRepository.findById(tutorId)).thenReturn(Optional.of(user));

        assertThat(service.tutorFullName(tutorId)).isEqualTo("Karim");
    }

    @Test
    void decryptedContent_decryptsTheStoredBlob() {
        UUID docId = UUID.randomUUID();
        VerificationDocument doc = unreviewedDocument(docId, UUID.randomUUID());
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(documentEncryptionService.decrypt(doc.getEncryptedBlob())).thenReturn("plaintext".getBytes());

        byte[] result = service.decryptedContent(docId);

        assertThat(result).isEqualTo("plaintext".getBytes());
    }

    @Test
    void documentMetadata_throwsNotFoundForUnknownDocument() {
        UUID docId = UUID.randomUUID();
        when(verificationDocumentRepository.findById(docId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.documentMetadata(docId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }
}

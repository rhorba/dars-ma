package ma.darsma.backend.profile;

import ma.darsma.backend.shared.security.DocumentEncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VerificationDocumentServiceTest {

    private VerificationDocumentRepository verificationDocumentRepository;
    private TutorProfileRepository tutorProfileRepository;
    private DocumentEncryptionService documentEncryptionService;
    private VerificationDocumentService service;

    @BeforeEach
    void setUp() {
        verificationDocumentRepository = mock(VerificationDocumentRepository.class);
        tutorProfileRepository = mock(TutorProfileRepository.class);
        documentEncryptionService = mock(DocumentEncryptionService.class);
        service = new VerificationDocumentService(verificationDocumentRepository, tutorProfileRepository, documentEncryptionService);
    }

    private TutorProfile pendingProfile(UUID tutorId) {
        return TutorProfile.builder()
                .userId(tutorId)
                .subjects(new String[]{"Math"})
                .hourlyRateMad(new java.math.BigDecimal("100.00"))
                .verificationStatus(VerificationStatus.PENDING)
                .build();
    }

    @Test
    void upload_encryptsAndStoresValidPdf() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(pendingProfile(tutorId)));
        when(documentEncryptionService.encrypt(any())).thenReturn(new byte[]{1, 2, 3});
        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "content".getBytes());

        VerificationDocument result = service.upload(tutorId, DocType.DIPLOMA, file);

        assertThat(result.getMimeType()).isEqualTo("application/pdf");
        assertThat(result.getEncryptedBlob()).containsExactly(1, 2, 3);
        verify(verificationDocumentRepository).save(any(VerificationDocument.class));
    }

    @Test
    void upload_rejectsDisallowedMimeType() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(pendingProfile(tutorId)));
        MockMultipartFile file = new MockMultipartFile("file", "script.exe", "application/x-msdownload", "content".getBytes());

        assertThatThrownBy(() -> service.upload(tutorId, DocType.DIPLOMA, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PDF, JPEG, or PNG");
        verifyNoInteractions(verificationDocumentRepository);
    }

    @Test
    void upload_rejectsOversizedFile() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(pendingProfile(tutorId)));
        byte[] oversized = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile("file", "big.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> service.upload(tutorId, DocType.DIPLOMA, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void upload_rejectsEmptyFile() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(pendingProfile(tutorId)));
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.upload(tutorId, DocType.DIPLOMA, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("empty");
    }

    @Test
    void upload_throwsNotFoundWhenNoProfileExists() {
        UUID tutorId = UUID.randomUUID();
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> service.upload(tutorId, DocType.DIPLOMA, file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void upload_doesNotDowngradeAlreadyVerifiedTutor() {
        UUID tutorId = UUID.randomUUID();
        TutorProfile verified = pendingProfile(tutorId);
        verified.setVerificationStatus(VerificationStatus.VERIFIED);
        when(tutorProfileRepository.findById(tutorId)).thenReturn(Optional.of(verified));
        when(documentEncryptionService.encrypt(any())).thenReturn(new byte[]{1});
        MockMultipartFile file = new MockMultipartFile("file", "diploma.pdf", "application/pdf", "content".getBytes());

        service.upload(tutorId, DocType.DIPLOMA, file);

        assertThat(verified.getVerificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        verify(tutorProfileRepository, never()).save(any(TutorProfile.class));
    }
}

package ma.darsma.backend.profile;

import ma.darsma.backend.shared.security.DocumentEncryptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class VerificationDocumentService {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("application/pdf", "image/jpeg", "image/png");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024;

    private final VerificationDocumentRepository verificationDocumentRepository;
    private final TutorProfileRepository tutorProfileRepository;
    private final DocumentEncryptionService documentEncryptionService;

    public VerificationDocumentService(VerificationDocumentRepository verificationDocumentRepository,
                                        TutorProfileRepository tutorProfileRepository,
                                        DocumentEncryptionService documentEncryptionService) {
        this.verificationDocumentRepository = verificationDocumentRepository;
        this.tutorProfileRepository = tutorProfileRepository;
        this.documentEncryptionService = documentEncryptionService;
    }

    @Transactional
    public VerificationDocument upload(UUID tutorUserId, DocType docType, MultipartFile file) {
        TutorProfile profile = tutorProfileRepository.findById(tutorUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tutor profile not found — create a profile before uploading documents"));

        if (file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "File is empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new ResponseStatusException(BAD_REQUEST, "File exceeds the 5MB size limit");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PDF, JPEG, or PNG files are accepted");
        }

        byte[] plaintext;
        try {
            plaintext = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(BAD_REQUEST, "Could not read uploaded file");
        }

        VerificationDocument document = VerificationDocument.builder()
                .tutorUserId(tutorUserId)
                .docType(docType)
                .encryptedBlob(documentEncryptionService.encrypt(plaintext))
                .mimeType(contentType)
                .originalFilename(file.getOriginalFilename())
                .build();
        verificationDocumentRepository.save(document);

        if (profile.getVerificationStatus() != VerificationStatus.VERIFIED) {
            profile.setVerificationStatus(VerificationStatus.PENDING);
            tutorProfileRepository.save(profile);
        }

        return document;
    }
}

package ma.darsma.backend.profile;

import ma.darsma.backend.profile.dto.VerificationDocumentResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile/tutor/me/verification-documents")
public class VerificationDocumentController {

    private final VerificationDocumentService verificationDocumentService;

    public VerificationDocumentController(VerificationDocumentService verificationDocumentService) {
        this.verificationDocumentService = verificationDocumentService;
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public VerificationDocumentResponse upload(Authentication authentication,
                                                @RequestParam DocType docType,
                                                @RequestParam MultipartFile file) {
        UUID tutorUserId = UUID.fromString(authentication.getName());
        return VerificationDocumentResponse.from(verificationDocumentService.upload(tutorUserId, docType, file));
    }
}

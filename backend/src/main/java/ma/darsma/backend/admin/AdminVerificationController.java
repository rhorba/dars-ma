package ma.darsma.backend.admin;

import jakarta.validation.Valid;
import ma.darsma.backend.admin.dto.RejectDocumentRequest;
import ma.darsma.backend.admin.dto.VerificationQueueItemResponse;
import ma.darsma.backend.profile.VerificationDocument;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/verification")
public class AdminVerificationController {

    private final AdminVerificationService adminVerificationService;

    public AdminVerificationController(AdminVerificationService adminVerificationService) {
        this.adminVerificationService = adminVerificationService;
    }

    @GetMapping("/queue")
    public List<VerificationQueueItemResponse> queue() {
        return adminVerificationService.pendingQueue().stream()
                .map(doc -> VerificationQueueItemResponse.from(doc, adminVerificationService.tutorFullName(doc.getTutorUserId())))
                .toList();
    }

    @GetMapping("/documents/{documentId}/content")
    public ResponseEntity<byte[]> content(@PathVariable UUID documentId) {
        VerificationDocument document = adminVerificationService.documentMetadata(documentId);
        byte[] content = adminVerificationService.decryptedContent(documentId);
        String filename = document.getOriginalFilename() != null ? document.getOriginalFilename() : "document";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + sanitizeForHeader(filename) + "\"")
                .body(content);
    }

    private static String sanitizeForHeader(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    @PostMapping("/documents/{documentId}/approve")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void approve(Authentication authentication, @PathVariable UUID documentId) {
        adminVerificationService.approve(documentId, UUID.fromString(authentication.getName()));
    }

    @PostMapping("/documents/{documentId}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(Authentication authentication, @PathVariable UUID documentId,
                        @Valid @RequestBody RejectDocumentRequest request) {
        adminVerificationService.reject(documentId, UUID.fromString(authentication.getName()), request.reason());
    }
}

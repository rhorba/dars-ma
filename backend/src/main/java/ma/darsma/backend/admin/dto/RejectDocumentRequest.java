package ma.darsma.backend.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectDocumentRequest(
        @NotBlank @Size(max = 500) String reason
) {
}

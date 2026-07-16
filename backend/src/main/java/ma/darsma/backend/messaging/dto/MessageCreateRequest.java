package ma.darsma.backend.messaging.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MessageCreateRequest(
        @NotBlank @Size(max = 5000) String body
) {
}

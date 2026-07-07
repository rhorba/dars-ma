package ma.darsma.backend.gig.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record GigRequestCreateRequest(
        @NotBlank @Size(max = 100) String subject,
        @NotBlank @Size(max = 50) String level,
        @NotBlank @Size(max = 5000) String description,
        @DecimalMin(value = "0.01") BigDecimal budgetMinMad,
        @DecimalMin(value = "0.01") BigDecimal budgetMaxMad
) {
}

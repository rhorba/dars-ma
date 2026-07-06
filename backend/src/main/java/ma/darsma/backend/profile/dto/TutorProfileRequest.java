package ma.darsma.backend.profile.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record TutorProfileRequest(
        @Size(max = 2000) String bio,
        @NotEmpty String[] subjects,
        @NotNull @DecimalMin(value = "0.01") BigDecimal hourlyRateMad
) {
}

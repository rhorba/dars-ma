package ma.darsma.backend.booking.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingCreateRequest(
        @NotNull UUID gigRequestId,
        @NotNull UUID tutorUserId,
        @NotNull @DecimalMin("0.5") @DecimalMax("12") BigDecimal durationHours
) {
}

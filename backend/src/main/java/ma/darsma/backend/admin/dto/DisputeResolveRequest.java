package ma.darsma.backend.admin.dto;

import jakarta.validation.constraints.NotNull;
import ma.darsma.backend.booking.DisputeResolution;

public record DisputeResolveRequest(
        @NotNull DisputeResolution resolution
) {
}

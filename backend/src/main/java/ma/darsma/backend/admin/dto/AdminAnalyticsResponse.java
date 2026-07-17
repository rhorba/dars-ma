package ma.darsma.backend.admin.dto;

import java.math.BigDecimal;

public record AdminAnalyticsResponse(
        long studentSignups,
        long tutorSignups,
        long totalBookings,
        long completedBookings,
        BigDecimal gmvMad,
        double matchRatePercent
) {
}

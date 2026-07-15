package ma.darsma.backend.booking.dto;

import ma.darsma.backend.booking.Booking;
import ma.darsma.backend.booking.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID gigRequestId,
        UUID studentUserId,
        UUID tutorUserId,
        BigDecimal agreedPriceMad,
        BookingStatus status,
        Instant studentConfirmedAt,
        Instant tutorConfirmedAt,
        Instant createdAt
) {
    public static BookingResponse from(Booking booking) {
        return new BookingResponse(
                booking.getId(),
                booking.getGigRequestId(),
                booking.getStudentUserId(),
                booking.getTutorUserId(),
                booking.getAgreedPriceMad(),
                booking.getStatus(),
                booking.getStudentConfirmedAt(),
                booking.getTutorConfirmedAt(),
                booking.getCreatedAt());
    }
}

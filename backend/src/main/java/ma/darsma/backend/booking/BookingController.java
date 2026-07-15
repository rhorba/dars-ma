package ma.darsma.backend.booking;

import jakarta.validation.Valid;
import ma.darsma.backend.booking.dto.BookingCreateRequest;
import ma.darsma.backend.booking.dto.BookingResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingCompletionService bookingCompletionService;

    public BookingController(BookingService bookingService, BookingCompletionService bookingCompletionService) {
        this.bookingService = bookingService;
        this.bookingCompletionService = bookingCompletionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BookingResponse create(Authentication authentication, @Valid @RequestBody BookingCreateRequest request) {
        UUID studentUserId = UUID.fromString(authentication.getName());
        return BookingResponse.from(bookingService.create(studentUserId, request));
    }

    @GetMapping("/{id}")
    public BookingResponse getOne(Authentication authentication, @PathVariable UUID id) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return BookingResponse.from(bookingService.getForParty(id, requesterId));
    }

    @PostMapping("/{id}/complete")
    public BookingResponse complete(Authentication authentication, @PathVariable UUID id) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return BookingResponse.from(bookingCompletionService.confirmCompletion(id, requesterId));
    }
}

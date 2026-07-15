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
    private final BookingDisputeService bookingDisputeService;

    public BookingController(BookingService bookingService, BookingCompletionService bookingCompletionService,
                              BookingDisputeService bookingDisputeService) {
        this.bookingService = bookingService;
        this.bookingCompletionService = bookingCompletionService;
        this.bookingDisputeService = bookingDisputeService;
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

    @PostMapping("/{id}/dispute")
    public BookingResponse dispute(Authentication authentication, @PathVariable UUID id) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return BookingResponse.from(bookingDisputeService.raise(id, requesterId));
    }
}

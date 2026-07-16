package ma.darsma.backend.review;

import jakarta.validation.Valid;
import ma.darsma.backend.review.dto.ReviewCreateRequest;
import ma.darsma.backend.review.dto.ReviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings/{bookingId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewResponse submit(Authentication authentication, @PathVariable UUID bookingId,
                                  @Valid @RequestBody ReviewCreateRequest request) {
        UUID reviewerId = UUID.fromString(authentication.getName());
        return ReviewResponse.from(reviewService.submit(bookingId, reviewerId, request.rating(), request.comment()));
    }

    @GetMapping
    public List<ReviewResponse> list(Authentication authentication, @PathVariable UUID bookingId) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return reviewService.getForBooking(bookingId, requesterId).stream().map(ReviewResponse::from).toList();
    }
}

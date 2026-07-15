package ma.darsma.backend.admin;

import jakarta.validation.Valid;
import ma.darsma.backend.admin.dto.DisputeResolveRequest;
import ma.darsma.backend.booking.dto.BookingResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/bookings")
public class AdminBookingDisputeController {

    private final AdminBookingDisputeService adminBookingDisputeService;

    public AdminBookingDisputeController(AdminBookingDisputeService adminBookingDisputeService) {
        this.adminBookingDisputeService = adminBookingDisputeService;
    }

    @GetMapping("/disputes")
    public List<BookingResponse> disputes() {
        return adminBookingDisputeService.disputedQueue().stream().map(BookingResponse::from).toList();
    }

    @PostMapping("/{id}/disputes/resolve")
    public BookingResponse resolve(Authentication authentication, @PathVariable UUID id,
                                    @Valid @RequestBody DisputeResolveRequest request) {
        UUID adminId = UUID.fromString(authentication.getName());
        return BookingResponse.from(adminBookingDisputeService.resolve(id, adminId, request.resolution()));
    }
}

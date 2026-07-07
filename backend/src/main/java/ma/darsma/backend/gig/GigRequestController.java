package ma.darsma.backend.gig;

import jakarta.validation.Valid;
import ma.darsma.backend.gig.dto.GigRequestCreateRequest;
import ma.darsma.backend.gig.dto.GigRequestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/gigs")
public class GigRequestController {

    private final GigRequestService gigRequestService;

    public GigRequestController(GigRequestService gigRequestService) {
        this.gigRequestService = gigRequestService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GigRequestResponse create(Authentication authentication, @Valid @RequestBody GigRequestCreateRequest request) {
        UUID studentUserId = UUID.fromString(authentication.getName());
        return GigRequestResponse.from(gigRequestService.create(studentUserId, request));
    }

    @GetMapping("/{id}")
    public GigRequestResponse getOwn(Authentication authentication, @PathVariable UUID id) {
        UUID requesterId = UUID.fromString(authentication.getName());
        return GigRequestResponse.from(gigRequestService.getForOwner(id, requesterId));
    }
}

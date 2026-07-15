package ma.darsma.backend.profile;

import jakarta.validation.Valid;
import ma.darsma.backend.profile.dto.TutorProfileRequest;
import ma.darsma.backend.profile.dto.TutorProfileResponse;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/profile/tutor")
public class TutorProfileController {

    private final TutorProfileService tutorProfileService;

    public TutorProfileController(TutorProfileService tutorProfileService) {
        this.tutorProfileService = tutorProfileService;
    }

    @PutMapping("/me")
    public TutorProfileResponse upsertOwn(Authentication authentication, @Valid @RequestBody TutorProfileRequest request) {
        UUID tutorUserId = UUID.fromString(authentication.getName());
        return TutorProfileResponse.from(tutorProfileService.upsert(tutorUserId, request));
    }

    @GetMapping("/me")
    public TutorProfileResponse getOwn(Authentication authentication) {
        UUID tutorUserId = UUID.fromString(authentication.getName());
        return TutorProfileResponse.from(tutorProfileService.getByUserId(tutorUserId));
    }

    @GetMapping("/{userId}")
    public TutorProfileResponse getPublic(@PathVariable UUID userId) {
        return TutorProfileResponse.from(tutorProfileService.getByUserId(userId));
    }

    @GetMapping
    public List<TutorProfileResponse> browse(@RequestParam(required = false) String subject) {
        return tutorProfileService.browseVerified(subject).stream().map(TutorProfileResponse::from).toList();
    }
}

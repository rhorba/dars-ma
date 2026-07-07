package ma.darsma.backend.profile;

import ma.darsma.backend.matching.EmbeddingService;
import ma.darsma.backend.profile.dto.TutorProfileRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class TutorProfileService {

    private final TutorProfileRepository tutorProfileRepository;
    private final EmbeddingService embeddingService;

    public TutorProfileService(TutorProfileRepository tutorProfileRepository, EmbeddingService embeddingService) {
        this.tutorProfileRepository = tutorProfileRepository;
        this.embeddingService = embeddingService;
    }

    @Transactional
    public TutorProfile upsert(UUID tutorUserId, TutorProfileRequest request) {
        TutorProfile profile = tutorProfileRepository.findById(tutorUserId)
                .orElseGet(() -> TutorProfile.builder().userId(tutorUserId).build());
        profile.setBio(request.bio());
        profile.setSubjects(request.subjects());
        profile.setHourlyRateMad(request.hourlyRateMad());
        TutorProfile saved = tutorProfileRepository.save(profile);
        embeddingService.embedTutorProfile(saved);
        return saved;
    }

    public TutorProfile getByUserId(UUID tutorUserId) {
        return tutorProfileRepository.findById(tutorUserId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Tutor profile not found"));
    }
}

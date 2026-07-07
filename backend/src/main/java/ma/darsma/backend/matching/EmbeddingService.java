package ma.darsma.backend.matching;

import ma.darsma.backend.gig.GigRequest;
import ma.darsma.backend.profile.TutorProfile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmbeddingService {

    private final EmbeddingProvider embeddingProvider;
    private final TutorEmbeddingRepository tutorEmbeddingRepository;
    private final GigEmbeddingRepository gigEmbeddingRepository;

    public EmbeddingService(
            EmbeddingProvider embeddingProvider,
            TutorEmbeddingRepository tutorEmbeddingRepository,
            GigEmbeddingRepository gigEmbeddingRepository) {
        this.embeddingProvider = embeddingProvider;
        this.tutorEmbeddingRepository = tutorEmbeddingRepository;
        this.gigEmbeddingRepository = gigEmbeddingRepository;
    }

    @Transactional
    public void embedTutorProfile(TutorProfile profile) {
        float[] vector = embeddingProvider.embed(buildTutorText(profile));
        TutorEmbedding embedding = tutorEmbeddingRepository.findById(profile.getUserId())
                .orElseGet(() -> new TutorEmbedding(profile.getUserId(), vector));
        embedding.setEmbedding(vector);
        tutorEmbeddingRepository.save(embedding);
    }

    @Transactional
    public void embedGigRequest(GigRequest gigRequest) {
        float[] vector = embeddingProvider.embed(buildGigText(gigRequest));
        GigEmbedding embedding = gigEmbeddingRepository.findById(gigRequest.getId())
                .orElseGet(() -> new GigEmbedding(gigRequest.getId(), vector));
        embedding.setEmbedding(vector);
        gigEmbeddingRepository.save(embedding);
    }

    private String buildTutorText(TutorProfile profile) {
        StringBuilder sb = new StringBuilder("Subjects: ").append(String.join(", ", profile.getSubjects()));
        if (profile.getBio() != null && !profile.getBio().isBlank()) {
            sb.append(". Bio: ").append(profile.getBio());
        }
        return sb.toString();
    }

    private String buildGigText(GigRequest gigRequest) {
        return "Subject: " + gigRequest.getSubject() + ". Level: " + gigRequest.getLevel() + ". " + gigRequest.getDescription();
    }
}

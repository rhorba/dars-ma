package ma.darsma.backend.matching;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GigEmbeddingRepository extends JpaRepository<GigEmbedding, UUID> {
}

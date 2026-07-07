package ma.darsma.backend.gig;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GigRequestRepository extends JpaRepository<GigRequest, UUID> {
}

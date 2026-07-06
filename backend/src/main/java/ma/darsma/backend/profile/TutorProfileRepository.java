package ma.darsma.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, UUID> {
}

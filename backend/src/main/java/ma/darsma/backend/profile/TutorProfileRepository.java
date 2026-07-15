package ma.darsma.backend.profile;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TutorProfileRepository extends JpaRepository<TutorProfile, UUID> {

    @Query(value = """
            SELECT user_id FROM tutor_profiles
            WHERE verification_status = 'VERIFIED' AND :subject = ANY(subjects)
            ORDER BY avg_rating DESC NULLS LAST
            LIMIT :limit
            """, nativeQuery = true)
    List<UUID> findVerifiedBySubjectFallback(@Param("subject") String subject, @Param("limit") int limit);

    @Query(value = """
            SELECT * FROM tutor_profiles
            WHERE verification_status = 'VERIFIED'
            ORDER BY avg_rating DESC NULLS LAST
            """, nativeQuery = true)
    List<TutorProfile> findAllVerified();

    @Query(value = """
            SELECT * FROM tutor_profiles
            WHERE verification_status = 'VERIFIED' AND :subject = ANY(subjects)
            ORDER BY avg_rating DESC NULLS LAST
            """, nativeQuery = true)
    List<TutorProfile> findVerifiedBySubject(@Param("subject") String subject);
}

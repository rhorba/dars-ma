package ma.darsma.backend.matching;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TutorEmbeddingRepository extends JpaRepository<TutorEmbedding, UUID> {

    @Query(value = """
            SELECT COUNT(*) FROM tutor_embeddings te
            JOIN tutor_profiles tp ON tp.user_id = te.tutor_user_id
            WHERE tp.verification_status = 'VERIFIED'
            """, nativeQuery = true)
    long countVerifiedWithEmbedding();

    @Query(value = """
            SELECT te.tutor_user_id AS tutorUserId, 1 - (te.embedding <=> CAST(:vector AS vector)) AS score
            FROM tutor_embeddings te
            JOIN tutor_profiles tp ON tp.user_id = te.tutor_user_id
            WHERE tp.verification_status = 'VERIFIED'
            ORDER BY te.embedding <=> CAST(:vector AS vector)
            LIMIT :limit
            """, nativeQuery = true)
    List<TutorMatchRow> findTopMatches(@Param("vector") String vector, @Param("limit") int limit);
}

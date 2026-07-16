package ma.darsma.backend.messaging;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MessageThreadRepository extends JpaRepository<MessageThread, UUID> {

    Optional<MessageThread> findByBookingId(UUID bookingId);
}

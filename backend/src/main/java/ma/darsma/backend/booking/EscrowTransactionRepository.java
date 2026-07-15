package ma.darsma.backend.booking;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, UUID> {

    Optional<EscrowTransaction> findByBookingId(UUID bookingId);
}

package ma.darsma.backend.booking;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Strategy interface for the escrow payment gateway (ADR-2). {@link MockEscrowPaymentProvider}
 * backs local/dev/test; a real CmiEscrowPaymentProvider is added once CMI sandbox access exists.
 */
public interface EscrowPaymentProvider {

    EscrowHoldResult hold(UUID bookingId, BigDecimal amountMad);

    void release(String cmiReference);

    void refund(String cmiReference);
}

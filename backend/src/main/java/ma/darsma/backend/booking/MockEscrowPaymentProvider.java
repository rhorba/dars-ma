package ma.darsma.backend.booking;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic stand-in for the real CMI gateway (CMI_MODE=mock, no sandbox access yet).
 * Hold always succeeds except for the well-known {@link #FAILURE_TRIGGER_AMOUNT_MAD} amount,
 * which lets tests exercise the payment-failure path without any randomness.
 */
@Component
public class MockEscrowPaymentProvider implements EscrowPaymentProvider {

    public static final BigDecimal FAILURE_TRIGGER_AMOUNT_MAD = new BigDecimal("999.99");

    @Override
    public EscrowHoldResult hold(UUID bookingId, BigDecimal amountMad) {
        if (amountMad.compareTo(FAILURE_TRIGGER_AMOUNT_MAD) == 0) {
            return EscrowHoldResult.failure();
        }
        return EscrowHoldResult.success("mock-" + bookingId);
    }

    @Override
    public void release(String cmiReference) {
        // no-op: mock gateway has no external state to reconcile
    }

    @Override
    public void refund(String cmiReference) {
        // no-op: mock gateway has no external state to reconcile
    }
}

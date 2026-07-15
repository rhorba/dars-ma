package ma.darsma.backend.booking;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class MockEscrowPaymentProviderTest {

    private final MockEscrowPaymentProvider provider = new MockEscrowPaymentProvider();

    @Test
    void hold_succeedsDeterministicallyForOrdinaryAmounts() {
        UUID bookingId = UUID.randomUUID();

        EscrowHoldResult result = provider.hold(bookingId, new BigDecimal("150.00"));

        assertThat(result.success()).isTrue();
        assertThat(result.cmiReference()).isEqualTo("mock-" + bookingId);
    }

    @Test
    void hold_failsDeterministicallyForTheTriggerAmount() {
        EscrowHoldResult result = provider.hold(UUID.randomUUID(), MockEscrowPaymentProvider.FAILURE_TRIGGER_AMOUNT_MAD);

        assertThat(result.success()).isFalse();
        assertThat(result.cmiReference()).isNull();
    }

    @Test
    void release_and_refund_doNotThrow() {
        provider.release("mock-ref");
        provider.refund("mock-ref");
    }
}

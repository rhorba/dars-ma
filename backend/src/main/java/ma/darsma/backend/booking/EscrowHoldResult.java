package ma.darsma.backend.booking;

public record EscrowHoldResult(boolean success, String cmiReference) {

    public static EscrowHoldResult success(String cmiReference) {
        return new EscrowHoldResult(true, cmiReference);
    }

    public static EscrowHoldResult failure() {
        return new EscrowHoldResult(false, null);
    }
}

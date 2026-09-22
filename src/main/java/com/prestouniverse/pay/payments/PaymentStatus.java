package com.prestouniverse.pay.payments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PaymentStatus {

    public static final PaymentStatus PENDING_AUTHORISE = new PaymentStatus("PendingAuthorise", true);
    public static final PaymentStatus CANCELLED = new PaymentStatus("Cancelled", true);
    public static final PaymentStatus AUTHORISED = new PaymentStatus("Authorised", true);
    public static final PaymentStatus FAILED = new PaymentStatus("Failed", true);
    public static final PaymentStatus PENDING_REVERSE = new PaymentStatus("PendingReverse", true);
    public static final PaymentStatus REVERSED = new PaymentStatus("Reversed", true);
    public static final PaymentStatus PENDING_REFUND = new PaymentStatus("PendingRefund", true);
    public static final PaymentStatus PARTIAL_REFUNDED = new PaymentStatus("PartialRefunded", true);
    public static final PaymentStatus REFUNDED = new PaymentStatus("Refunded", true);
    public static final PaymentStatus EXPIRED = new PaymentStatus("Expired", true);

    private static final Map<String, PaymentStatus> KNOWN = new LinkedHashMap<>();

    static {
        register(PENDING_AUTHORISE);
        register(CANCELLED);
        register(AUTHORISED);
        register(FAILED);
        register(PENDING_REVERSE);
        register(REVERSED);
        register(PENDING_REFUND);
        register(PARTIAL_REFUNDED);
        register(REFUNDED);
        register(EXPIRED);
    }

    private final String value;
    private final boolean known;

    private PaymentStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(PaymentStatus status) {
        KNOWN.put(status.value, status);
    }

    public static PaymentStatus of(String value) {
        Objects.requireNonNull(value, "value");
        PaymentStatus match = KNOWN.get(value);
        return match != null ? match : new PaymentStatus(value, false);
    }

    public String value() {
        return value;
    }

    public boolean isKnown() {
        return known;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof PaymentStatus)) {
            return false;
        }
        return value.equals(((PaymentStatus) obj).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}

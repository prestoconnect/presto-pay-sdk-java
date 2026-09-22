package com.prestouniverse.pay.payments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class RefundStatus {

    public static final RefundStatus REFUNDING = new RefundStatus("Refunding", true);
    public static final RefundStatus FAILED = new RefundStatus("Failed", true);
    public static final RefundStatus SUCCESS = new RefundStatus("Success", true);

    private static final Map<String, RefundStatus> KNOWN = new LinkedHashMap<>();

    static {
        register(REFUNDING);
        register(FAILED);
        register(SUCCESS);
    }

    private final String value;
    private final boolean known;

    private RefundStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(RefundStatus status) {
        KNOWN.put(status.value, status);
    }

    public static RefundStatus of(String value) {
        Objects.requireNonNull(value, "value");
        RefundStatus match = KNOWN.get(value);
        return match != null ? match : new RefundStatus(value, false);
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
        if (!(obj instanceof RefundStatus)) {
            return false;
        }
        return value.equals(((RefundStatus) obj).value);
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

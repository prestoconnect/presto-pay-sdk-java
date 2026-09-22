package com.prestouniverse.pay.payments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class ReversalStatus {

    public static final ReversalStatus REVERSING = new ReversalStatus("Reversing", true);
    public static final ReversalStatus FAILED = new ReversalStatus("Failed", true);
    public static final ReversalStatus SUCCESS = new ReversalStatus("Success", true);

    private static final Map<String, ReversalStatus> KNOWN = new LinkedHashMap<>();

    static {
        register(REVERSING);
        register(FAILED);
        register(SUCCESS);
    }

    private final String value;
    private final boolean known;

    private ReversalStatus(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(ReversalStatus status) {
        KNOWN.put(status.value, status);
    }

    public static ReversalStatus of(String value) {
        Objects.requireNonNull(value, "value");
        ReversalStatus match = KNOWN.get(value);
        return match != null ? match : new ReversalStatus(value, false);
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
        if (!(obj instanceof ReversalStatus)) {
            return false;
        }
        return value.equals(((ReversalStatus) obj).value);
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

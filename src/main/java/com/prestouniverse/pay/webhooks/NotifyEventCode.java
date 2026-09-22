package com.prestouniverse.pay.webhooks;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class NotifyEventCode {

    public static final NotifyEventCode AUTHORISED = new NotifyEventCode("Authorised", true);
    public static final NotifyEventCode CANCELLED = new NotifyEventCode("Cancelled", true);
    public static final NotifyEventCode REVERSED = new NotifyEventCode("Reversed", true);
    public static final NotifyEventCode REFUNDED = new NotifyEventCode("Refunded", true);
    public static final NotifyEventCode EXPIRED = new NotifyEventCode("Expired", true);

    private static final Map<String, NotifyEventCode> KNOWN = new LinkedHashMap<>();

    static {
        register(AUTHORISED);
        register(CANCELLED);
        register(REVERSED);
        register(REFUNDED);
        register(EXPIRED);
    }

    private final String value;
    private final boolean known;

    private NotifyEventCode(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(NotifyEventCode code) {
        KNOWN.put(code.value, code);
    }

    public static NotifyEventCode of(String value) {
        Objects.requireNonNull(value, "value");
        NotifyEventCode match = KNOWN.get(value);
        return match != null ? match : new NotifyEventCode(value, false);
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
        if (!(obj instanceof NotifyEventCode)) {
            return false;
        }
        return value.equals(((NotifyEventCode) obj).value);
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

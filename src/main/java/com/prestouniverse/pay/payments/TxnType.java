package com.prestouniverse.pay.payments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TxnType {

    public static final TxnType QR_PAY = new TxnType("QrPay", true);
    public static final TxnType WEB_PAY = new TxnType("WebPay", true);
    public static final TxnType MINI_APP_PAY = new TxnType("MiniAppPay", true);

    private static final Map<String, TxnType> KNOWN = new LinkedHashMap<>();

    static {
        register(QR_PAY);
        register(WEB_PAY);
        register(MINI_APP_PAY);
    }

    private final String value;
    private final boolean known;

    private TxnType(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(TxnType type) {
        KNOWN.put(type.value, type);
    }

    public static TxnType of(String value) {
        Objects.requireNonNull(value, "value");
        TxnType match = KNOWN.get(value);
        return match != null ? match : new TxnType(value, false);
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
        if (!(obj instanceof TxnType)) {
            return false;
        }
        return value.equals(((TxnType) obj).value);
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

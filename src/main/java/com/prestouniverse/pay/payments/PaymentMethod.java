package com.prestouniverse.pay.payments;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class PaymentMethod {

    public static final PaymentMethod WALLET = new PaymentMethod("Wallet", true);
    public static final PaymentMethod CASH_BACK = new PaymentMethod("CashBack", true);
    public static final PaymentMethod CARD = new PaymentMethod("Card", true);
    public static final PaymentMethod BIG_LIFE = new PaymentMethod("BigLife", true);
    public static final PaymentMethod BONUS_LINK = new PaymentMethod("BonusLink", true);
    public static final PaymentMethod RISE = new PaymentMethod("RISE", true);
    public static final PaymentMethod SUBWALLET_BUDDY = new PaymentMethod("Subwallet_BUDDY", true);
    public static final PaymentMethod PM_PG_CARD = new PaymentMethod("PmPgCard", true);
    public static final PaymentMethod MAYBANK = new PaymentMethod("Maybank", true);
    public static final PaymentMethod AMBANK = new PaymentMethod("Ambank", true);
    public static final PaymentMethod RHB = new PaymentMethod("Rhb", true);
    public static final PaymentMethod HONG_LEONG = new PaymentMethod("HongLeong", true);
    public static final PaymentMethod CIMB = new PaymentMethod("Cimb", true);
    public static final PaymentMethod PUBLIC_BANK = new PaymentMethod("PublicBank", true);
    public static final PaymentMethod AFFIN_BANK = new PaymentMethod("AffinBank", true);
    public static final PaymentMethod BSN = new PaymentMethod("Bsn", true);
    public static final PaymentMethod HONG_LEONG_PEX = new PaymentMethod("HongLeongPex", true);
    public static final PaymentMethod UNION_PAY = new PaymentMethod("UnionPay", true);
    public static final PaymentMethod BOOST = new PaymentMethod("Boost", true);
    public static final PaymentMethod GRAB_PAY = new PaymentMethod("GrabPay", true);
    public static final PaymentMethod TOUCH_N_GO = new PaymentMethod("TouchNGo", true);
    public static final PaymentMethod TOUCH_N_GO_EWALLET = new PaymentMethod("TouchNGoEWallet", true);
    public static final PaymentMethod ALIPAY_CHINA = new PaymentMethod("AliPayChina", true);
    public static final PaymentMethod LATITUDE_PAY = new PaymentMethod("LatitudePay", true);

    private static final Map<String, PaymentMethod> KNOWN = new LinkedHashMap<>();

    static {
        register(WALLET);
        register(CASH_BACK);
        register(CARD);
        register(BIG_LIFE);
        register(BONUS_LINK);
        register(RISE);
        register(SUBWALLET_BUDDY);
        register(PM_PG_CARD);
        register(MAYBANK);
        register(AMBANK);
        register(RHB);
        register(HONG_LEONG);
        register(CIMB);
        register(PUBLIC_BANK);
        register(AFFIN_BANK);
        register(BSN);
        register(HONG_LEONG_PEX);
        register(UNION_PAY);
        register(BOOST);
        register(GRAB_PAY);
        register(TOUCH_N_GO);
        register(TOUCH_N_GO_EWALLET);
        register(ALIPAY_CHINA);
        register(LATITUDE_PAY);
    }

    private final String value;
    private final boolean known;

    private PaymentMethod(String value, boolean known) {
        this.value = value;
        this.known = known;
    }

    private static void register(PaymentMethod method) {
        KNOWN.put(method.value, method);
    }

    public static PaymentMethod of(String value) {
        Objects.requireNonNull(value, "value");
        PaymentMethod match = KNOWN.get(value);
        return match != null ? match : new PaymentMethod(value, false);
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
        if (!(obj instanceof PaymentMethod)) {
            return false;
        }
        return value.equals(((PaymentMethod) obj).value);
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

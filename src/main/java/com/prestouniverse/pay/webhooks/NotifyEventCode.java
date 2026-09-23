package com.prestouniverse.pay.webhooks;

public final class NotifyEventCode {

    public static final String AUTHORISED = "Authorised";
    public static final String CANCELLED = "Cancelled";
    public static final String REVERSED = "Reversed";
    public static final String REFUNDED = "Refunded";
    public static final String EXPIRED = "Expired";

    private NotifyEventCode() {
    }
}

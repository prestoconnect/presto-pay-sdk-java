package com.prestouniverse.pay.webhooks;

/** Known notify event codes for {@link NotifyEvent#eventCode()}. The gateway may send values not listed here. */
public final class NotifyEventCode {

    public static final String AUTHORISED = "Authorised";
    public static final String CANCELLED = "Cancelled";
    public static final String REVERSED = "Reversed";
    public static final String REFUNDED = "Refunded";
    public static final String EXPIRED = "Expired";

    private NotifyEventCode() {
    }
}

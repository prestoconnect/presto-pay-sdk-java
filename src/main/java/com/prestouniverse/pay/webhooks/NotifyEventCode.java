package com.prestouniverse.pay.webhooks;

/** Known notify event codes for {@link NotifyEvent#eventCode()}. The gateway may send values not listed here. */
public final class NotifyEventCode {

    public static final String Authorised = "Authorised";
    public static final String Cancelled = "Cancelled";
    public static final String Reversed = "Reversed";
    public static final String Refunded = "Refunded";
    public static final String Expired = "Expired";

    private NotifyEventCode() {
    }
}

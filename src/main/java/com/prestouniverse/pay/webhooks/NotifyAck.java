package com.prestouniverse.pay.webhooks;

/**
 * Response bodies for acknowledging a webhook. Return HTTP 200 with {@link #ok()} once the event is safely
 * recorded.
 */
public final class NotifyAck {

    private static final String OK = "{\"resend\":false}";
    private static final String RESEND = "{\"resend\":true}";

    private NotifyAck() {
    }

    public static String ok() {
        return OK;
    }

    public static String resend() {
        return RESEND;
    }
}

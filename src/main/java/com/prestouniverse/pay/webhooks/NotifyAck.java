package com.prestouniverse.pay.webhooks;

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

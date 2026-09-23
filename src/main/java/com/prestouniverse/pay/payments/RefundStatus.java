package com.prestouniverse.pay.payments;

/** Known refund status values. The gateway may return values not listed here. */
public final class RefundStatus {

    public static final String Refunding = "Refunding";
    public static final String Failed = "Failed";
    public static final String Success = "Success";

    private RefundStatus() {
    }
}

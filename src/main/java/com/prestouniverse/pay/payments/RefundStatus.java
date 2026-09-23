package com.prestouniverse.pay.payments;

/** Known refund status values. The gateway may return values not listed here. */
public final class RefundStatus {

    public static final String REFUNDING = "Refunding";
    public static final String FAILED = "Failed";
    public static final String SUCCESS = "Success";

    private RefundStatus() {
    }
}

package com.prestouniverse.pay.payments;

public final class PaymentStatus {

    public static final String PENDING_AUTHORISE = "PendingAuthorise";
    public static final String CANCELLED = "Cancelled";
    public static final String AUTHORISED = "Authorised";
    public static final String FAILED = "Failed";
    public static final String PENDING_REVERSE = "PendingReverse";
    public static final String REVERSED = "Reversed";
    public static final String PENDING_REFUND = "PendingRefund";
    public static final String PARTIAL_REFUNDED = "PartialRefunded";
    public static final String REFUNDED = "Refunded";
    public static final String EXPIRED = "Expired";

    private PaymentStatus() {
    }
}

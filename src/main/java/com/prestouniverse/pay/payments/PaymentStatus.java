package com.prestouniverse.pay.payments;

/** Known payment status values. The gateway may return values not listed here. */
public final class PaymentStatus {

    public static final String PendingAuthorise = "PendingAuthorise";
    public static final String Cancelled = "Cancelled";
    public static final String Authorised = "Authorised";
    public static final String Failed = "Failed";
    public static final String PendingReverse = "PendingReverse";
    public static final String Reversed = "Reversed";
    public static final String PendingRefund = "PendingRefund";
    public static final String PartialRefunded = "PartialRefunded";
    public static final String Refunded = "Refunded";
    public static final String Expired = "Expired";

    private PaymentStatus() {
    }
}

package com.prestouniverse.pay.payments;

/** Known reversal status values. The gateway may return values not listed here. */
public final class ReversalStatus {

    public static final String Reversing = "Reversing";
    public static final String Failed = "Failed";
    public static final String Success = "Success";

    private ReversalStatus() {
    }
}

package com.prestouniverse.pay.payments;

/** Known reversal status values. The gateway may return values not listed here. */
public final class ReversalStatus {

    public static final String REVERSING = "Reversing";
    public static final String FAILED = "Failed";
    public static final String SUCCESS = "Success";

    private ReversalStatus() {
    }
}

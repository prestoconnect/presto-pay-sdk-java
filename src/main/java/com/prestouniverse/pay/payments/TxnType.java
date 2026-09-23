package com.prestouniverse.pay.payments;

/** Known transaction types for {@link PaymentInitRequest.Builder#txnType(String)}. */
public final class TxnType {

    public static final String QrPay = "QrPay";
    public static final String WebPay = "WebPay";
    public static final String MiniAppPay = "MiniAppPay";

    private TxnType() {
    }
}

package com.prestouniverse.pay.payments;

/** Known transaction types for {@link PaymentInitRequest.Builder#txnType(String)}. */
public final class TxnType {

    public static final String QR_PAY = "QrPay";
    public static final String WEB_PAY = "WebPay";
    public static final String MINI_APP_PAY = "MiniAppPay";

    private TxnType() {
    }
}

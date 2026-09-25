package com.prestouniverse.pay.sample.demo.controller.support;

/** JSON body returned by {@code POST /checkout} on success. */
public final class CheckoutResponse {

    private final String paymentUrl;
    private final String txnRefNum;

    public CheckoutResponse(String paymentUrl, String txnRefNum) {
        this.paymentUrl = paymentUrl;
        this.txnRefNum = txnRefNum;
    }

    public String getPaymentUrl() {
        return paymentUrl;
    }

    public String getTxnRefNum() {
        return txnRefNum;
    }
}

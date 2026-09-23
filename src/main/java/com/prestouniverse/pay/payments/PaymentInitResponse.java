package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentInitResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String txnRefNum;
    private final String paymentStatus;
    private final String paymentUrl;
    private final String userRefNum;
    private final Integer amount;
    private final String currencyCode;
    private final String paymentRequestDate;
    private final String paymentFinalisedDate;
    private final String additionalData;
    private final String ts;

    private PaymentInitResponse(String prestoMrn, String paymentRefNum, String txnRefNum, String paymentStatus,
            String paymentUrl, String userRefNum, Integer amount, String currencyCode, String paymentRequestDate,
            String paymentFinalisedDate, String additionalData, String ts) {
        this.prestoMrn = prestoMrn;
        this.paymentRefNum = paymentRefNum;
        this.txnRefNum = txnRefNum;
        this.paymentStatus = paymentStatus;
        this.paymentUrl = paymentUrl;
        this.userRefNum = userRefNum;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paymentRequestDate = paymentRequestDate;
        this.paymentFinalisedDate = paymentFinalisedDate;
        this.additionalData = additionalData;
        this.ts = ts;
    }

    static PaymentInitResponse fromJson(JsonObject node) {
        return new PaymentInitResponse(
                JsonCodec.text(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "txnRefNum"),
                JsonCodec.requiredText(node, "paymentStatus"),
                JsonCodec.text(node, "paymentUrl"),
                JsonCodec.text(node, "userRefNum"),
                JsonCodec.optInt(node, "amount"),
                JsonCodec.text(node, "currencyCode"),
                JsonCodec.text(node, "paymentRequestDate"),
                JsonCodec.text(node, "paymentFinalisedDate"),
                JsonCodec.text(node, "additionalData"),
                JsonCodec.text(node, "ts"));
    }

    public String prestoMrn() {
        return prestoMrn;
    }

    public String paymentRefNum() {
        return paymentRefNum;
    }

    public String txnRefNum() {
        return txnRefNum;
    }

    public String paymentStatus() {
        return paymentStatus;
    }

    public String paymentUrl() {
        return paymentUrl;
    }

    public String userRefNum() {
        return userRefNum;
    }

    /** Amount in minor currency units, or {@code null} if the gateway omitted it. */
    public Integer amount() {
        return amount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String paymentRequestDate() {
        return paymentRequestDate;
    }

    public String paymentFinalisedDate() {
        return paymentFinalisedDate;
    }

    public String additionalData() {
        return additionalData;
    }

    public String ts() {
        return ts;
    }

    @Override
    public String toString() {
        return "PaymentInitResponse{paymentRefNum=" + paymentRefNum + ", txnRefNum=" + txnRefNum
                + ", paymentStatus=" + paymentStatus + ", amount=" + amount + ", currencyCode=" + currencyCode + '}';
    }
}

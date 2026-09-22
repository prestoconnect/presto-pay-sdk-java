package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentInitResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String txnRefNum;
    private final PaymentStatus paymentStatus;
    private final String paymentUrl;
    private final String userRefNum;
    private final int amount;
    private final String currencyCode;
    private final String paymentRequestDate;
    private final String paymentFinalisedDate;
    private final String additionalData;
    private final String ts;

    private PaymentInitResponse(String prestoMrn, String paymentRefNum, String txnRefNum,
            PaymentStatus paymentStatus, String paymentUrl, String userRefNum, int amount, String currencyCode,
            String paymentRequestDate, String paymentFinalisedDate, String additionalData, String ts) {
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

    public static PaymentInitResponse fromJson(JsonObject node) {
        return new PaymentInitResponse(
                JsonCodec.requiredText(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.requiredText(node, "txnRefNum"),
                PaymentStatus.of(JsonCodec.requiredText(node, "paymentStatus")),
                JsonCodec.text(node, "paymentUrl"),
                JsonCodec.text(node, "userRefNum"),
                JsonCodec.requiredInt(node, "amount"),
                JsonCodec.requiredText(node, "currencyCode"),
                JsonCodec.requiredText(node, "paymentRequestDate"),
                JsonCodec.text(node, "paymentFinalisedDate"),
                JsonCodec.text(node, "additionalData"),
                JsonCodec.requiredText(node, "ts"));
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

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String paymentUrl() {
        return paymentUrl;
    }

    public String userRefNum() {
        return userRefNum;
    }

    public int amount() {
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
}

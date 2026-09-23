package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentReverseResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String prestoReversalRefNum;
    private final int amount;
    private final String currencyCode;
    private final PaymentStatus paymentStatus;
    private final String ts;

    private PaymentReverseResponse(String prestoMrn, String paymentRefNum, String prestoReversalRefNum, int amount,
            String currencyCode, PaymentStatus paymentStatus, String ts) {
        this.prestoMrn = prestoMrn;
        this.paymentRefNum = paymentRefNum;
        this.prestoReversalRefNum = prestoReversalRefNum;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paymentStatus = paymentStatus;
        this.ts = ts;
    }

    static PaymentReverseResponse fromJson(JsonObject node) {
        Integer amount = JsonCodec.optInt(node, "amount");
        String status = JsonCodec.text(node, "paymentStatus");
        return new PaymentReverseResponse(
                JsonCodec.text(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "prestoReversalRefNum"),
                amount != null ? amount : 0,
                JsonCodec.text(node, "currencyCode"),
                status != null ? PaymentStatus.of(status) : null,
                JsonCodec.text(node, "ts"));
    }

    public String prestoMrn() {
        return prestoMrn;
    }

    public String paymentRefNum() {
        return paymentRefNum;
    }

    public String prestoReversalRefNum() {
        return prestoReversalRefNum;
    }

    public int amount() {
        return amount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String ts() {
        return ts;
    }
}

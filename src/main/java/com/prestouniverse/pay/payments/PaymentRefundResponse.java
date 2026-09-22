package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentRefundResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String prestoRefundRefNum;
    private final int amount;
    private final int refundAmount;
    private final String currencyCode;
    private final PaymentStatus paymentStatus;
    private final String refundedDate;
    private final String ts;

    private PaymentRefundResponse(String prestoMrn, String paymentRefNum, String prestoRefundRefNum, int amount,
            int refundAmount, String currencyCode, PaymentStatus paymentStatus, String refundedDate, String ts) {
        this.prestoMrn = prestoMrn;
        this.paymentRefNum = paymentRefNum;
        this.prestoRefundRefNum = prestoRefundRefNum;
        this.amount = amount;
        this.refundAmount = refundAmount;
        this.currencyCode = currencyCode;
        this.paymentStatus = paymentStatus;
        this.refundedDate = refundedDate;
        this.ts = ts;
    }

    public static PaymentRefundResponse fromJson(JsonObject node) {
        Integer amount = JsonCodec.optInt(node, "amount");
        Integer refundAmount = JsonCodec.optInt(node, "refundAmount");
        String status = JsonCodec.text(node, "paymentStatus");
        return new PaymentRefundResponse(
                JsonCodec.text(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "prestoRefundRefNum"),
                amount != null ? amount : 0,
                refundAmount != null ? refundAmount : 0,
                JsonCodec.text(node, "currencyCode"),
                status != null ? PaymentStatus.of(status) : null,
                JsonCodec.text(node, "refundedDate"),
                JsonCodec.text(node, "ts"));
    }

    public String prestoMrn() {
        return prestoMrn;
    }

    public String paymentRefNum() {
        return paymentRefNum;
    }

    public String prestoRefundRefNum() {
        return prestoRefundRefNum;
    }

    public int amount() {
        return amount;
    }

    public int refundAmount() {
        return refundAmount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public PaymentStatus paymentStatus() {
        return paymentStatus;
    }

    public String refundedDate() {
        return refundedDate;
    }

    public String ts() {
        return ts;
    }
}

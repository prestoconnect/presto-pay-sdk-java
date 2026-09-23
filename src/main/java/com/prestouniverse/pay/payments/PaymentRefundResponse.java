package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentRefundResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String prestoRefundRefNum;
    private final Integer amount;
    private final Integer refundAmount;
    private final String currencyCode;
    private final String paymentStatus;
    private final String refundedDate;
    private final String ts;

    private PaymentRefundResponse(String prestoMrn, String paymentRefNum, String prestoRefundRefNum, Integer amount,
            Integer refundAmount, String currencyCode, String paymentStatus, String refundedDate, String ts) {
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

    static PaymentRefundResponse fromJson(JsonObject node) {
        return new PaymentRefundResponse(
                JsonCodec.text(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "prestoRefundRefNum"),
                JsonCodec.optInt(node, "amount"),
                JsonCodec.optInt(node, "refundAmount"),
                JsonCodec.text(node, "currencyCode"),
                JsonCodec.text(node, "paymentStatus"),
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

    /** Original payment amount in minor currency units, or {@code null} if the gateway omitted it. */
    public Integer amount() {
        return amount;
    }

    /** Refunded amount in minor currency units, or {@code null} if the gateway omitted it. */
    public Integer refundAmount() {
        return refundAmount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String paymentStatus() {
        return paymentStatus;
    }

    public String refundedDate() {
        return refundedDate;
    }

    public String ts() {
        return ts;
    }

    @Override
    public String toString() {
        return "PaymentRefundResponse{paymentRefNum=" + paymentRefNum + ", prestoRefundRefNum=" + prestoRefundRefNum
                + ", paymentStatus=" + paymentStatus + ", amount=" + amount + ", refundAmount=" + refundAmount
                + ", currencyCode=" + currencyCode + '}';
    }
}

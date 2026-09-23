package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

/** Result of {@link PaymentsClient#reverse}. */
public final class PaymentReverseResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String prestoReversalRefNum;
    private final Integer amount;
    private final String currencyCode;
    private final String paymentStatus;
    private final String ts;

    private PaymentReverseResponse(String prestoMrn, String paymentRefNum, String prestoReversalRefNum, Integer amount,
            String currencyCode, String paymentStatus, String ts) {
        this.prestoMrn = prestoMrn;
        this.paymentRefNum = paymentRefNum;
        this.prestoReversalRefNum = prestoReversalRefNum;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.paymentStatus = paymentStatus;
        this.ts = ts;
    }

    static PaymentReverseResponse fromJson(JsonObject node) {
        return new PaymentReverseResponse(
                JsonCodec.text(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "prestoReversalRefNum"),
                JsonCodec.optInt(node, "amount"),
                JsonCodec.text(node, "currencyCode"),
                JsonCodec.text(node, "paymentStatus"),
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

    /** Amount in minor currency units, or {@code null} if the gateway omitted it. */
    public Integer amount() {
        return amount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String paymentStatus() {
        return paymentStatus;
    }

    public String ts() {
        return ts;
    }

    @Override
    public String toString() {
        return "PaymentReverseResponse{paymentRefNum=" + paymentRefNum + ", prestoReversalRefNum="
                + prestoReversalRefNum + ", paymentStatus=" + paymentStatus + ", amount=" + amount
                + ", currencyCode=" + currencyCode + '}';
    }
}

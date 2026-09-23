package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** How (part of) a payment was paid: method, amount in minor units, and card details where applicable. */
public final class PaymentDetail {

    private final String method;
    private final int amount;
    private final String cardBin;
    private final String cardSummary;
    private final String cardType;
    private final String refNum;

    private PaymentDetail(String method, int amount, String cardBin, String cardSummary, String cardType,
            String refNum) {
        this.method = method;
        this.amount = amount;
        this.cardBin = cardBin;
        this.cardSummary = cardSummary;
        this.cardType = cardType;
        this.refNum = refNum;
    }

    static List<PaymentDetail> parseList(String json) {
        List<PaymentDetail> details = new ArrayList<>();
        for (JsonObject node : JsonCodec.parseObjectArray(json)) {
            details.add(fromJson(node));
        }
        return details;
    }

    private static PaymentDetail fromJson(JsonObject node) {
        return new PaymentDetail(
                JsonCodec.text(node, "method"),
                JsonCodec.requiredInt(node, "amount"),
                JsonCodec.text(node, "cardBin"),
                JsonCodec.text(node, "cardSummary"),
                JsonCodec.text(node, "cardType"),
                JsonCodec.text(node, "refNum"));
    }

    public String method() {
        return method;
    }

    public int amount() {
        return amount;
    }

    public String cardBin() {
        return cardBin;
    }

    public String cardSummary() {
        return cardSummary;
    }

    public String cardType() {
        return cardType;
    }

    public String refNum() {
        return refNum;
    }

    @Override
    public String toString() {
        return "PaymentDetail{method=" + method + ", amount=" + amount + ", refNum=" + refNum + '}';
    }
}

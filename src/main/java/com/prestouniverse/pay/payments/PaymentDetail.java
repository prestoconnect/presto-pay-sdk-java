package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class PaymentDetail {

    private final PaymentMethod method;
    private final int amount;
    private final String cardBin;
    private final String cardSummary;
    private final String cardType;

    private PaymentDetail(PaymentMethod method, int amount, String cardBin, String cardSummary, String cardType) {
        this.method = method;
        this.amount = amount;
        this.cardBin = cardBin;
        this.cardSummary = cardSummary;
        this.cardType = cardType;
    }

    public static List<PaymentDetail> parseList(String json) {
        List<PaymentDetail> details = new ArrayList<>();
        for (JsonObject node : JsonCodec.parseObjectArray(json)) {
            details.add(fromJson(node));
        }
        return details;
    }

    private static PaymentDetail fromJson(JsonObject node) {
        return new PaymentDetail(
                PaymentMethod.of(JsonCodec.requiredText(node, "method")),
                JsonCodec.requiredInt(node, "amount"),
                JsonCodec.text(node, "cardBin"),
                JsonCodec.text(node, "cardSummary"),
                JsonCodec.text(node, "cardType"));
    }

    public PaymentMethod method() {
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
}

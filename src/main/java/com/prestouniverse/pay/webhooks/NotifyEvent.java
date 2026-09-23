package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentDetail;

import java.util.List;

public final class NotifyEvent {

    private final NotifyEventCode eventCode;
    private final String mid;
    private final String prestoMrn;
    private final String paymentRefNum;
    private final String userRefNum;
    private final String txnRefNum;
    private final boolean success;
    private final String eventRefNum;
    private final String eventTs;
    private final int amount;
    private final String currencyCode;
    private final String additionalData;
    private final List<PaymentDetail> paymentDetails;
    private final String ts;

    private NotifyEvent(NotifyEventCode eventCode, String mid, String prestoMrn, String paymentRefNum,
            String userRefNum, String txnRefNum, boolean success, String eventRefNum, String eventTs, int amount,
            String currencyCode, String additionalData, List<PaymentDetail> paymentDetails, String ts) {
        this.eventCode = eventCode;
        this.mid = mid;
        this.prestoMrn = prestoMrn;
        this.paymentRefNum = paymentRefNum;
        this.userRefNum = userRefNum;
        this.txnRefNum = txnRefNum;
        this.success = success;
        this.eventRefNum = eventRefNum;
        this.eventTs = eventTs;
        this.amount = amount;
        this.currencyCode = currencyCode;
        this.additionalData = additionalData;
        this.paymentDetails = paymentDetails;
        this.ts = ts;
    }

    static NotifyEvent fromJson(JsonObject node) {
        String paymentDetailsJson = JsonCodec.text(node, "paymentDetails");
        return new NotifyEvent(
                NotifyEventCode.of(JsonCodec.requiredText(node, "eventCode")),
                JsonCodec.requiredText(node, "mid"),
                JsonCodec.requiredText(node, "prestoMrn"),
                JsonCodec.requiredText(node, "paymentRefNum"),
                JsonCodec.text(node, "userRefNum"),
                JsonCodec.requiredText(node, "txnRefNum"),
                JsonCodec.optBoolean(node, "success", false),
                JsonCodec.requiredText(node, "eventRefNum"),
                JsonCodec.requiredText(node, "eventTs"),
                JsonCodec.requiredInt(node, "amount"),
                JsonCodec.requiredText(node, "currencyCode"),
                JsonCodec.text(node, "additionalData"),
                PaymentDetail.parseList(paymentDetailsJson != null ? paymentDetailsJson : "[]"),
                JsonCodec.requiredText(node, "ts"));
    }

    public NotifyEventCode eventCode() {
        return eventCode;
    }

    public String mid() {
        return mid;
    }

    public String prestoMrn() {
        return prestoMrn;
    }

    public String paymentRefNum() {
        return paymentRefNum;
    }

    public String userRefNum() {
        return userRefNum;
    }

    public String txnRefNum() {
        return txnRefNum;
    }

    public boolean success() {
        return success;
    }

    public String eventRefNum() {
        return eventRefNum;
    }

    public String eventTs() {
        return eventTs;
    }

    public int amount() {
        return amount;
    }

    public String currencyCode() {
        return currencyCode;
    }

    public String additionalData() {
        return additionalData;
    }

    public List<PaymentDetail> paymentDetails() {
        return paymentDetails;
    }

    public String ts() {
        return ts;
    }
}

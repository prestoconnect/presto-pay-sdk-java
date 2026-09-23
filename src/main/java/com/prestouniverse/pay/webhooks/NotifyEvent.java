package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.SdkAccess;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentDetail;
import com.prestouniverse.pay.payments.PaymentStatus;

import java.util.Collections;
import java.util.List;

/** A verified notify webhook. Use {@code payments().query()} for authoritative payment state. */
public final class NotifyEvent {

    private final String eventCode;
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

    private NotifyEvent(String eventCode, String mid, String prestoMrn, String paymentRefNum,
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
        this.paymentDetails = Collections.unmodifiableList(paymentDetails);
        this.ts = ts;
    }

    static NotifyEvent fromJson(JsonObject node) {
        String paymentDetailsJson = JsonCodec.text(node, "paymentDetails");
        return new NotifyEvent(
                JsonCodec.requiredText(node, "eventCode"),
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
                SdkAccess.payments().parsePaymentDetails(paymentDetailsJson != null ? paymentDetailsJson : "[]"),
                JsonCodec.requiredText(node, "ts"));
    }

    public String eventCode() {
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

    /**
     * Suggested {@link com.prestouniverse.pay.payments.PaymentStatus} value from this notify event.
     * For {@link NotifyEventCode#AUTHORISED}, uses {@link #success()}. Call {@code payments().query()} for
     * authoritative payment status after handling a webhook.
     */
    public String paymentStatus() {
        if (NotifyEventCode.AUTHORISED.equals(eventCode)) {
            return success() ? PaymentStatus.AUTHORISED : PaymentStatus.FAILED;
        }
        return eventCode;
    }

    @Override
    public String toString() {
        return "NotifyEvent{eventCode=" + eventCode + ", eventRefNum=" + eventRefNum + ", mid=" + mid
                + ", paymentRefNum=" + paymentRefNum + ", txnRefNum=" + txnRefNum + ", success=" + success
                + ", amount=" + amount + ", currencyCode=" + currencyCode + '}';
    }
}

package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

/** One refund against a payment, as listed in {@link PaymentQueryResponse#refundDetails()}. */
public final class RefundDetail {

    private final String refundRefNum;
    private final String prestoRefundRefNum;
    private final String refundStatus;
    private final String refundRequestDate;
    private final String refundFinalisedDate;

    private RefundDetail(String refundRefNum, String prestoRefundRefNum, String refundStatus,
            String refundRequestDate, String refundFinalisedDate) {
        this.refundRefNum = refundRefNum;
        this.prestoRefundRefNum = prestoRefundRefNum;
        this.refundStatus = refundStatus;
        this.refundRequestDate = refundRequestDate;
        this.refundFinalisedDate = refundFinalisedDate;
    }

    static List<RefundDetail> parseList(String json) {
        List<RefundDetail> details = new ArrayList<>();
        for (JsonObject node : JsonCodec.parseObjectArray(json)) {
            details.add(fromJson(node));
        }
        return details;
    }

    private static RefundDetail fromJson(JsonObject node) {
        return new RefundDetail(
                JsonCodec.requiredText(node, "refundRefNum"),
                JsonCodec.requiredText(node, "prestoRefundRefNum"),
                JsonCodec.requiredText(node, "refundStatus"),
                JsonCodec.requiredText(node, "refundRequestDate"),
                JsonCodec.text(node, "refundFinalisedDate"));
    }

    public String refundRefNum() {
        return refundRefNum;
    }

    public String prestoRefundRefNum() {
        return prestoRefundRefNum;
    }

    public String refundStatus() {
        return refundStatus;
    }

    public String refundRequestDate() {
        return refundRequestDate;
    }

    public String refundFinalisedDate() {
        return refundFinalisedDate;
    }

    @Override
    public String toString() {
        return "RefundDetail{refundRefNum=" + refundRefNum + ", prestoRefundRefNum=" + prestoRefundRefNum
                + ", refundStatus=" + refundStatus + '}';
    }
}

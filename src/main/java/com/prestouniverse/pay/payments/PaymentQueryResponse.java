package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.Collections;
import java.util.List;

public final class PaymentQueryResponse {

    private final String prestoMrn;
    private final String paymentRefNum;
    private final String txnRefNum;
    private final String userRefNum;
    private final String paymentStatus;
    private final Integer amount;
    private final String currencyCode;
    private final String paymentRequestDate;
    private final String paymentFinalisedDate;
    private final String reversalRefNum;
    private final String prestoReversalRefNum;
    private final String reversalStatus;
    private final String reversalDate;
    private final String refundRefNum;
    private final String prestoRefundRefNum;
    private final String refundStatus;
    private final String refundRequestDate;
    private final String refundFinalisedDate;
    private final String additionalData;
    private final List<RefundDetail> refundDetails;
    private final List<PaymentDetail> paymentDetails;
    private final String ts;

    private PaymentQueryResponse(Builder builder) {
        this.prestoMrn = builder.prestoMrn;
        this.paymentRefNum = builder.paymentRefNum;
        this.txnRefNum = builder.txnRefNum;
        this.userRefNum = builder.userRefNum;
        this.paymentStatus = builder.paymentStatus;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.paymentRequestDate = builder.paymentRequestDate;
        this.paymentFinalisedDate = builder.paymentFinalisedDate;
        this.reversalRefNum = builder.reversalRefNum;
        this.prestoReversalRefNum = builder.prestoReversalRefNum;
        this.reversalStatus = builder.reversalStatus;
        this.reversalDate = builder.reversalDate;
        this.refundRefNum = builder.refundRefNum;
        this.prestoRefundRefNum = builder.prestoRefundRefNum;
        this.refundStatus = builder.refundStatus;
        this.refundRequestDate = builder.refundRequestDate;
        this.refundFinalisedDate = builder.refundFinalisedDate;
        this.additionalData = builder.additionalData;
        this.refundDetails = Collections.unmodifiableList(builder.refundDetails);
        this.paymentDetails = Collections.unmodifiableList(builder.paymentDetails);
        this.ts = builder.ts;
    }

    static PaymentQueryResponse fromJson(JsonObject node) {
        Builder builder = new Builder();
        builder.prestoMrn = JsonCodec.text(node, "prestoMrn");
        builder.paymentRefNum = JsonCodec.requiredText(node, "paymentRefNum");
        builder.txnRefNum = JsonCodec.text(node, "txnRefNum");
        builder.userRefNum = JsonCodec.text(node, "userRefNum");
        builder.paymentStatus = JsonCodec.text(node, "paymentStatus");
        builder.amount = JsonCodec.optInt(node, "amount");
        builder.currencyCode = JsonCodec.text(node, "currencyCode");
        builder.paymentRequestDate = JsonCodec.text(node, "paymentRequestDate");
        builder.paymentFinalisedDate = JsonCodec.text(node, "paymentFinalisedDate");
        builder.reversalRefNum = JsonCodec.text(node, "reversalRefNum");
        builder.prestoReversalRefNum = JsonCodec.text(node, "prestoReversalRefNum");
        builder.reversalStatus = JsonCodec.text(node, "reversalStatus");
        builder.reversalDate = JsonCodec.text(node, "reversalDate");
        builder.refundRefNum = JsonCodec.text(node, "refundRefNum");
        builder.prestoRefundRefNum = JsonCodec.text(node, "prestoRefundRefNum");
        builder.refundStatus = JsonCodec.text(node, "refundStatus");
        builder.refundRequestDate = JsonCodec.text(node, "refundRequestDate");
        builder.refundFinalisedDate = JsonCodec.text(node, "refundFinalisedDate");
        builder.additionalData = JsonCodec.text(node, "additionalData");
        String refundDetailsJson = JsonCodec.text(node, "refundDetails");
        builder.refundDetails = RefundDetail.parseList(refundDetailsJson != null ? refundDetailsJson : "[]");
        String paymentDetailsJson = JsonCodec.text(node, "paymentDetails");
        builder.paymentDetails = PaymentDetail.parseList(paymentDetailsJson != null ? paymentDetailsJson : "[]");
        builder.ts = JsonCodec.text(node, "ts");
        return new PaymentQueryResponse(builder);
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

    public String userRefNum() {
        return userRefNum;
    }

    public String paymentStatus() {
        return paymentStatus;
    }

    /** Amount in minor currency units, or {@code null} if the gateway omitted it. */
    public Integer amount() {
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

    public String reversalRefNum() {
        return reversalRefNum;
    }

    public String prestoReversalRefNum() {
        return prestoReversalRefNum;
    }

    public String reversalStatus() {
        return reversalStatus;
    }

    public String reversalDate() {
        return reversalDate;
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

    public String additionalData() {
        return additionalData;
    }

    public List<RefundDetail> refundDetails() {
        return refundDetails;
    }

    public List<PaymentDetail> paymentDetails() {
        return paymentDetails;
    }

    public String ts() {
        return ts;
    }

    @Override
    public String toString() {
        return "PaymentQueryResponse{paymentRefNum=" + paymentRefNum + ", txnRefNum=" + txnRefNum
                + ", paymentStatus=" + paymentStatus + ", amount=" + amount + ", currencyCode=" + currencyCode
                + ", reversalStatus=" + reversalStatus + ", refundStatus=" + refundStatus + '}';
    }

    private static final class Builder {
        private String prestoMrn;
        private String paymentRefNum;
        private String txnRefNum;
        private String userRefNum;
        private String paymentStatus;
        private Integer amount;
        private String currencyCode;
        private String paymentRequestDate;
        private String paymentFinalisedDate;
        private String reversalRefNum;
        private String prestoReversalRefNum;
        private String reversalStatus;
        private String reversalDate;
        private String refundRefNum;
        private String prestoRefundRefNum;
        private String refundStatus;
        private String refundRequestDate;
        private String refundFinalisedDate;
        private String additionalData;
        private List<RefundDetail> refundDetails;
        private List<PaymentDetail> paymentDetails;
        private String ts;
    }
}

package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

/**
 * Refunds all or part of a payment ({@code /v1/ext/payment/refund}). Not idempotent: after an ambiguous failure,
 * {@code query} before retrying.
 */
public final class PaymentRefundRequest {

    private final String merchantRefNum;
    private final String paymentRefNum;
    private final String refundRefNum;
    private final String remark;
    private final String notifyUrl;
    private final Integer amount;

    private PaymentRefundRequest(Builder builder) {
        this.merchantRefNum = builder.merchantRefNum;
        this.paymentRefNum = builder.paymentRefNum;
        this.refundRefNum = builder.refundRefNum;
        this.remark = builder.remark;
        this.notifyUrl = builder.notifyUrl;
        this.amount = builder.amount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String merchantRefNum() {
        return merchantRefNum;
    }

    JsonObject toJson() {
        JsonObject node = JsonCodec.newObject();
        node.put("paymentRefNum", paymentRefNum);
        node.put("refundRefNum", refundRefNum);
        node.put("remark", remark);
        JsonCodec.putIfPresent(node, "notifyUrl", notifyUrl);
        JsonCodec.putIfPresent(node, "amount", amount);
        return node;
    }

    public static final class Builder {

        private String merchantRefNum;
        private String paymentRefNum;
        private String refundRefNum;
        private String remark;
        private String notifyUrl;
        private Integer amount;

        private Builder() {
        }

        public Builder merchantRefNum(String merchantRefNum) {
            this.merchantRefNum = merchantRefNum;
            return this;
        }

        public Builder paymentRefNum(String paymentRefNum) {
            this.paymentRefNum = paymentRefNum;
            return this;
        }

        public Builder refundRefNum(String refundRefNum) {
            this.refundRefNum = refundRefNum;
            return this;
        }

        public Builder remark(String remark) {
            this.remark = remark;
            return this;
        }

        public Builder notifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public PaymentRefundRequest build() {
            Validation.requireNonBlank("merchantRefNum", merchantRefNum);
            Validation.requireNonNull("paymentRefNum", paymentRefNum);
            Validation.requireNonNull("refundRefNum", refundRefNum);
            Validation.requireNonNull("remark", remark);
            Validation.requireMaxLength("refundRefNum", refundRefNum, 50);
            Validation.requireMaxLength("remark", remark, 200);
            Validation.requireMaxLength("notifyUrl", notifyUrl, 255);
            Validation.requirePositive("amount", amount);
            return new PaymentRefundRequest(this);
        }
    }
}

package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

/**
 * Reverses (voids) a payment ({@code /v1/ext/payment/reverse}). Not idempotent: after an ambiguous failure, {@code
 * query} before retrying.
 */
public final class PaymentReverseRequest {

    private final String merchantRefNum;
    private final String paymentRefNum;
    private final String txnRefNum;
    private final String reversalRefNum;
    private final String remark;
    private final String notifyUrl;

    private PaymentReverseRequest(Builder builder) {
        this.merchantRefNum = builder.merchantRefNum;
        this.paymentRefNum = builder.paymentRefNum;
        this.txnRefNum = builder.txnRefNum;
        this.reversalRefNum = builder.reversalRefNum;
        this.remark = builder.remark;
        this.notifyUrl = builder.notifyUrl;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String merchantRefNum() {
        return merchantRefNum;
    }

    JsonObject toJson() {
        JsonObject node = JsonCodec.newObject();
        JsonCodec.putIfPresent(node, "paymentRefNum", paymentRefNum);
        JsonCodec.putIfPresent(node, "txnRefNum", txnRefNum);
        node.put("reversalRefNum", reversalRefNum);
        JsonCodec.putIfPresent(node, "remark", remark);
        JsonCodec.putIfPresent(node, "notifyUrl", notifyUrl);
        return node;
    }

    public static final class Builder {

        private String merchantRefNum;
        private String paymentRefNum;
        private String txnRefNum;
        private String reversalRefNum;
        private String remark;
        private String notifyUrl;

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

        public Builder txnRefNum(String txnRefNum) {
            this.txnRefNum = txnRefNum;
            return this;
        }

        public Builder reversalRefNum(String reversalRefNum) {
            this.reversalRefNum = reversalRefNum;
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

        public PaymentReverseRequest build() {
            Validation.requireNonBlank("merchantRefNum", merchantRefNum);
            Validation.requireNonNull("reversalRefNum", reversalRefNum);
            Validation.requireMaxLength("reversalRefNum", reversalRefNum, 50);
            Validation.requireMaxLength("remark", remark, 200);
            Validation.requireMaxLength("notifyUrl", notifyUrl, 255);
            if (paymentRefNum == null && txnRefNum == null) {
                throw new PrestoPayConfigException("paymentRefNum",
                        "either paymentRefNum or txnRefNum is required");
            }
            return new PaymentReverseRequest(this);
        }
    }
}

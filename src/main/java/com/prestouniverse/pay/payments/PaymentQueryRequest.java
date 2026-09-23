package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;

public final class PaymentQueryRequest {

    private final String merchantId;
    private final String merchantRefNum;
    private final String paymentRefNum;
    private final String txnRefNum;

    private PaymentQueryRequest(Builder builder) {
        this.merchantId = builder.merchantId;
        this.merchantRefNum = builder.merchantRefNum;
        this.paymentRefNum = builder.paymentRefNum;
        this.txnRefNum = builder.txnRefNum;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String merchantId() {
        return merchantId;
    }

    public String merchantRefNum() {
        return merchantRefNum;
    }

    JsonObject toJson() {
        JsonObject node = JsonCodec.newObject();
        JsonCodec.putIfPresent(node, "paymentRefNum", paymentRefNum);
        JsonCodec.putIfPresent(node, "txnRefNum", txnRefNum);
        return node;
    }

    public static final class Builder {

        private String merchantId;
        private String merchantRefNum;
        private String paymentRefNum;
        private String txnRefNum;

        private Builder() {
        }

        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
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

        public PaymentQueryRequest build() {
            if (paymentRefNum == null && txnRefNum == null) {
                throw new PrestoPayConfigException("paymentRefNum",
                        "either paymentRefNum or txnRefNum is required");
            }
            return new PaymentQueryRequest(this);
        }
    }
}

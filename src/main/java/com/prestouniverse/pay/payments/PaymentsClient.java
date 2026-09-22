package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.internal.RequestPipeline;

public final class PaymentsClient {

    private static final String INIT_PATH = "/v1/ext/payment/init";
    private static final String QUERY_PATH = "/v1/ext/payment/query";
    private static final String REVERSE_PATH = "/v1/ext/payment/reverse";
    private static final String REFUND_PATH = "/v1/ext/payment/refund";

    private final RequestPipeline pipeline;

    public PaymentsClient(RequestPipeline pipeline) {
        this.pipeline = pipeline;
    }

    public PaymentInitResponse init(PaymentInitParams params) {
        return PaymentInitResponse.fromJson(
                pipeline.execute(INIT_PATH, params.toJson(), params.merchantId(), params.merchantRefNum(), false));
    }

    public PaymentQueryResponse query(PaymentQueryParams params) {
        return PaymentQueryResponse.fromJson(
                pipeline.execute(QUERY_PATH, params.toJson(), params.merchantId(), params.merchantRefNum(), true));
    }

    public PaymentReverseResponse reverse(PaymentReverseParams params) {
        return PaymentReverseResponse.fromJson(pipeline.execute(
                REVERSE_PATH, params.toJson(), params.merchantId(), params.merchantRefNum(), false));
    }

    public PaymentRefundResponse refund(PaymentRefundParams params) {
        return PaymentRefundResponse.fromJson(
                pipeline.execute(REFUND_PATH, params.toJson(), params.merchantId(), params.merchantRefNum(), false));
    }
}

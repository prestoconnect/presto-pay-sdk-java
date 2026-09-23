package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPayResponseException.Source;
import com.prestouniverse.pay.internal.RequestPipeline;
import com.prestouniverse.pay.internal.SdkAccess;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.function.Function;

/** Payment operations for one merchant. Obtain from {@code PrestoPayClient.payments()}; thread-safe. */
public final class PaymentsClient {

    private static final String INIT_PATH = "/v1/ext/payment/init";
    private static final String QUERY_PATH = "/v1/ext/payment/query";
    private static final String REVERSE_PATH = "/v1/ext/payment/reverse";
    private static final String REFUND_PATH = "/v1/ext/payment/refund";

    static {
        SdkAccess.registerPayments(new PaymentsAccess());
    }

    private final RequestPipeline pipeline;

    PaymentsClient(RequestPipeline pipeline) {
        this.pipeline = pipeline;
    }

    /**
     * Starts a payment. Retried only if the request certainly was not sent; after any other failure, call
     * {@link #query} with the same {@code txnRefNum} instead of calling {@code init} again.
     *
     * @throws com.prestouniverse.pay.exception.PrestoPayApiException if the gateway rejects the request
     * @throws com.prestouniverse.pay.exception.PrestoPayTransportException on network failure
     * @throws com.prestouniverse.pay.exception.PrestoPayResponseException if the response cannot be parsed; the
     *         payment may still have been created
     */
    public PaymentInitResponse init(PaymentInitRequest request) {
        return parse(PaymentInitResponse::fromJson, pipeline.execute(
                INIT_PATH, request.toJson(), request.merchantRefNum(), false));
    }

    /**
     * Returns the current state of a payment. Read-only; retried on transport errors and HTTP 5xx according to
     * the client's {@code RetryPolicy}.
     */
    public PaymentQueryResponse query(PaymentQueryRequest request) {
        return parse(PaymentQueryResponse::fromJson, pipeline.execute(
                QUERY_PATH, request.toJson(), request.merchantRefNum(), true));
    }

    /**
     * Reverses a payment. Retried only if the request certainly was not sent; after any other failure, check
     * the reversal status with {@link #query} before retrying.
     */
    public PaymentReverseResponse reverse(PaymentReverseRequest request) {
        return parse(PaymentReverseResponse::fromJson, pipeline.execute(
                REVERSE_PATH, request.toJson(), request.merchantRefNum(), false));
    }

    /**
     * Refunds all or part of a payment. Retried only if the request certainly was not sent; after any other
     * failure, check the refund status with {@link #query} before retrying.
     */
    public PaymentRefundResponse refund(PaymentRefundRequest request) {
        return parse(PaymentRefundResponse::fromJson, pipeline.execute(
                REFUND_PATH, request.toJson(), request.merchantRefNum(), false));
    }

    private static <T> T parse(Function<JsonObject, T> parser, JsonObject node) {
        try {
            return parser.apply(node);
        } catch (IllegalArgumentException e) {
            throw new PrestoPayResponseException(Source.RESPONSE, "Invalid response body: " + e.getMessage(),
                    node.toJson(), e);
        }
    }
}

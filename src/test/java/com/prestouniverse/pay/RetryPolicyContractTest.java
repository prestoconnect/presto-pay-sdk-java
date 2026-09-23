package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.internal.JdkHttpTransport;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentRefundRequest;
import com.prestouniverse.pay.payments.PaymentReverseRequest;
import com.prestouniverse.pay.payments.TxnType;
import com.prestouniverse.pay.support.FlakyTransport;
import com.prestouniverse.pay.support.MockGatewayServer;
import com.prestouniverse.pay.support.TestKeys;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryPolicyContractTest {

    private static final String MID = "PW2401XH9KCX";
    private static final String MRN = "PM240110XDSFC";

    private MockGatewayServer mockGateway;

    @BeforeEach
    void setUp() {
        mockGateway = MockGatewayServer.start();
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", JsonCodec.text(request, "prestoMrn"));
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", "TXN10001");
            response.put("paymentStatus", "PendingAuthorise");
            response.put("paymentUrl", "https://hpp-staging.prestouniverse.com/PM/PP250423ND56NHO");
            response.put("amount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("ts", "20250423104530.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });
    }

    @AfterEach
    void tearDown() {
        mockGateway.close();
    }

    @Test
    void queryRetriesAfterATransportFailureEvenWhenTheRequestMayHaveBeenSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, false);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        client.payments().query(PaymentQueryRequest.builder()
                .merchantRefNum(MRN)
                .paymentRefNum("PP250423ND56NHO")
                .build());

        assertEquals(2, transport.callCount());
    }

    @Test
    void initDoesNotRetryWhenTheRequestMayHaveBeenSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, false);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().init(initRequest()));
        assertEquals(1, transport.callCount());
    }

    @Test
    void initRetriesWhenTheRequestDefinitelyWasNotSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, true);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        client.payments().init(initRequest());

        assertEquals(2, transport.callCount());
    }

    @Test
    void retriesAreBoundedByMaxRetries() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 5, true);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().init(initRequest()));
        assertEquals(3, transport.callCount());
    }

    @Test
    void queryRetriesOnServerError() {
        AtomicInteger calls = failFirstRequestsWith(503, 1);
        PrestoPayClient client = clientWith(new FlakyTransport(new JdkHttpTransport(), 0, false),
                RetryPolicy.of(2, Duration.ZERO));

        client.payments().query(queryRequest());

        assertEquals(2, calls.get());
    }

    @Test
    void queryDoesNotRetryOnClientError() {
        AtomicInteger calls = failFirstRequestsWith(400, 1);
        PrestoPayClient client = clientWith(new FlakyTransport(new JdkHttpTransport(), 0, false),
                RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayApiException.class, () -> client.payments().query(queryRequest()));
        assertEquals(1, calls.get());
    }

    @Test
    void initDoesNotRetryOnServerError() {
        AtomicInteger calls = failFirstRequestsWith(503, 1);
        PrestoPayClient client = clientWith(new FlakyTransport(new JdkHttpTransport(), 0, false),
                RetryPolicy.of(2, Duration.ZERO));

        PrestoPayApiException error = assertThrows(PrestoPayApiException.class,
                () -> client.payments().init(initRequest()));
        assertEquals(503, error.httpStatus());
        assertEquals(1, calls.get());
    }

    @Test
    void reverseDoesNotRetryWhenTheRequestMayHaveBeenSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, false);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().reverse(
                PaymentReverseRequest.builder()
                        .merchantRefNum(MRN)
                        .paymentRefNum("PP250423ND56NHO")
                        .reversalRefNum("REV1")
                        .build()));
        assertEquals(1, transport.callCount());
    }

    @Test
    void refundDoesNotRetryWhenTheRequestMayHaveBeenSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, false);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().refund(
                PaymentRefundRequest.builder()
                        .merchantRefNum(MRN)
                        .paymentRefNum("PP250423ND56NHO")
                        .refundRefNum("RFD1")
                        .remark("damaged")
                        .build()));
        assertEquals(1, transport.callCount());
    }

    @Test
    void retryPolicyRejectsInvalidArguments() {
        assertEquals("maxRetries", assertThrows(PrestoPayConfigException.class,
                () -> RetryPolicy.of(-1, Duration.ZERO)).field());
        assertEquals("initialBackoff", assertThrows(PrestoPayConfigException.class,
                () -> RetryPolicy.of(1, null)).field());
        assertEquals("initialBackoff", assertThrows(PrestoPayConfigException.class,
                () -> RetryPolicy.of(1, Duration.ofMillis(-1))).field());
    }

    private AtomicInteger failFirstRequestsWith(int status, int failures) {
        AtomicInteger calls = new AtomicInteger();
        MockGatewayServer.Handler success = mockGateway.currentHandler();
        mockGateway.handler(request -> calls.incrementAndGet() <= failures
                ? MockGatewayServer.Response.systemError(status, "9999", "Gateway error")
                : success.handle(request));
        return calls;
    }

    private PaymentQueryRequest queryRequest() {
        return PaymentQueryRequest.builder()
                .merchantRefNum(MRN)
                .paymentRefNum("PP250423ND56NHO")
                .build();
    }

    private PaymentInitRequest initRequest() {
        return PaymentInitRequest.builder()
                .merchantRefNum(MRN)
                .txnType(TxnType.QrPay)
                .txnRefNum("TXN10001")
                .displayDesc("Order #12345")
                .build();
    }

    private PrestoPayClient clientWith(FlakyTransport transport, RetryPolicy retryPolicy) {
        return PrestoPayClient.builder()
                .baseUrl(mockGateway.baseUrl())
                .merchantId(MID)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .transport(transport)
                .retryPolicy(retryPolicy)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
    }
}

package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.internal.JdkHttpTransport;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentInitParams;
import com.prestouniverse.pay.payments.PaymentQueryParams;
import com.prestouniverse.pay.payments.TxnType;
import com.prestouniverse.pay.support.FlakyTransport;
import com.prestouniverse.pay.support.MockGatewayServer;
import com.prestouniverse.pay.support.TestKeys;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetryPolicyContractTest {

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

        client.payments().query(PaymentQueryParams.builder().paymentRefNum("PP250423ND56NHO").build());

        assertEquals(2, transport.callCount());
    }

    @Test
    void initDoesNotRetryWhenTheRequestMayHaveBeenSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, false);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().init(initParams()));
        assertEquals(1, transport.callCount());
    }

    @Test
    void initRetriesWhenTheRequestDefinitelyWasNotSent() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 1, true);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        client.payments().init(initParams());

        assertEquals(2, transport.callCount());
    }

    @Test
    void retriesAreBoundedByMaxRetries() {
        FlakyTransport transport = new FlakyTransport(new JdkHttpTransport(), 5, true);
        PrestoPayClient client = clientWith(transport, RetryPolicy.of(2, Duration.ZERO));

        assertThrows(PrestoPayTransportException.class, () -> client.payments().init(initParams()));
        assertEquals(3, transport.callCount());
    }

    private PaymentInitParams initParams() {
        return PaymentInitParams.builder()
                .txnType(TxnType.QR_PAY)
                .txnRefNum("TXN10001")
                .displayDesc("Order #12345")
                .build();
    }

    private PrestoPayClient clientWith(FlakyTransport transport, RetryPolicy retryPolicy) {
        return PrestoPayClient.builder()
                .baseUrl(mockGateway.baseUrl())
                .merchantId("PW2401XH9KCX")
                .merchantRefNum("PM240110XDSFC")
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .transport(transport)
                .retryPolicy(retryPolicy)
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
    }
}

package com.prestouniverse.pay;

import com.prestouniverse.pay.crypto.Canonicalizer;
import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.Timestamps;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.payments.PaymentRefundRequest;
import com.prestouniverse.pay.payments.PaymentRefundResponse;
import com.prestouniverse.pay.payments.PaymentReverseRequest;
import com.prestouniverse.pay.payments.PaymentReverseResponse;
import com.prestouniverse.pay.payments.PaymentStatus;
import com.prestouniverse.pay.payments.TxnType;
import com.prestouniverse.pay.support.MockGatewayServer;
import com.prestouniverse.pay.support.TestKeys;
import com.prestouniverse.pay.webhooks.NotifyEvent;
import com.prestouniverse.pay.webhooks.WebhookVerifier;

import java.time.Clock;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PrestoPayClientContractTest {

    private static final String MID = "PW2401XH9KCX";
    private static final String MRN = "PM240110XDSFC";

    private MockGatewayServer mockGateway;
    private PrestoPayClient client;

    @BeforeEach
    void setUp() {
        mockGateway = MockGatewayServer.start();
        client = PrestoPayClient.builder()
                .baseUrl(mockGateway.baseUrl())
                .merchantId(MID)
                .merchantRefNum(MRN)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .retryPolicy(RetryPolicy.none())
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    void tearDown() {
        mockGateway.close();
    }

    @Test
    void initRequestIsSignedAndResponseIsVerifiedAndParsed() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", JsonCodec.text(request, "prestoMrn"));
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", JsonCodec.text(request, "txnRefNum"));
            response.put("paymentStatus", "PendingAuthorise");
            response.put("paymentUrl", "https://hpp-staging.prestouniverse.com/PM240110XDSFC/PP250423ND56NHO");
            response.put("amount", JsonCodec.requiredInt(request, "amount"));
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("ts", "20250423104530.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentInitResponse response = client.payments().init(PaymentInitRequest.builder()
                .txnType(TxnType.WEB_PAY)
                .txnRefNum("TXN10001")
                .displayDesc("Order #12345")
                .amount(1200)
                .currencyCode("MYR")
                .notifyUrl("https://merchant.example.com/webhook/notify")
                .redirectUrl("https://merchant.example.com/redirect/TXN10001")
                .build());

        assertEquals("PP250423ND56NHO", response.paymentRefNum());
        assertEquals(PaymentStatus.PENDING_AUTHORISE, response.paymentStatus());
        assertEquals(1200, response.amount());

        JsonObject sentRequest = mockGateway.lastRequestBody();
        assertEquals(MID, JsonCodec.text(sentRequest, "mid"));
        assertEquals(MRN, JsonCodec.text(sentRequest, "prestoMrn"));
        assertRequestIsCorrectlySigned(sentRequest);
        assertTrue(mockGateway.lastRequestHeaders().get("User-Agent").startsWith("presto-pay-sdk/"));

        assertEquals("application/json; charset=UTF-8", mockGateway.lastRequestHeaders().get("Content-Type"));
    }

    @Test
    void chineseDisplayDescSurvivesARealHttpRoundTripAndSignatureVerification() {
        String displayDesc = "訂單 #12345 測試，客戶備註：請盡快處理 😀";

        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", JsonCodec.text(request, "prestoMrn"));
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", JsonCodec.text(request, "txnRefNum"));
            response.put("paymentStatus", "PendingAuthorise");
            response.put("paymentUrl", "https://hpp-staging.prestouniverse.com/PM240110XDSFC/PP250423ND56NHO");
            response.put("amount", JsonCodec.requiredInt(request, "amount"));
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("additionalData", JsonCodec.text(request, "displayDesc"));
            response.put("ts", "20250423104530.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentInitResponse response = client.payments().init(PaymentInitRequest.builder()
                .txnType(TxnType.WEB_PAY)
                .txnRefNum("TXN10001")
                .displayDesc(displayDesc)
                .amount(1200)
                .currencyCode("MYR")
                .notifyUrl("https://merchant.example.com/webhook/notify")
                .redirectUrl("https://merchant.example.com/redirect/TXN10001")
                .build());

        assertEquals(displayDesc, JsonCodec.text(mockGateway.lastRequestBody(), "displayDesc"));
        assertEquals(displayDesc, response.additionalData());
    }

    @Test
    void queryRoundTrip() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", "TXN10001");
            response.put("paymentStatus", "Authorised");
            response.put("amount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("paymentFinalisedDate", "20250423105020.000");
            response.put("refundDetails", "[]");
            response.put("paymentDetails", "[{\"method\":\"Wallet\",\"amount\":1200}]");
            response.put("ts", "20250423105030.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentQueryResponse response = client.payments().query(
                PaymentQueryRequest.builder().paymentRefNum("PP250423ND56NHO").build());

        assertEquals(PaymentStatus.AUTHORISED, response.paymentStatus());
        assertEquals(1, response.paymentDetails().size());
        assertEquals(PaymentMethod.WALLET, response.paymentDetails().get(0).method());
        assertTrue(response.refundDetails().isEmpty());
    }

    @Test
    void queryPaymentDetailMayOmitMethod() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", "TXN10001");
            response.put("paymentStatus", "Authorised");
            response.put("amount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("paymentFinalisedDate", "20250423105020.000");
            response.put("refundDetails", "[]");
            response.put("paymentDetails", "[{\"amount\":1200}]");
            response.put("ts", "20250423105030.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentQueryResponse response = client.payments().query(
                PaymentQueryRequest.builder().paymentRefNum("PP250423ND56NHO").build());

        assertNull(response.paymentDetails().get(0).method());
    }

    @Test
    void reverseRoundTrip() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("prestoReversalRefNum", "REV10001");
            response.put("amount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentStatus", "Reversed");
            response.put("ts", "20250423105130.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentReverseResponse response = client.payments().reverse(PaymentReverseRequest.builder()
                .paymentRefNum("PP250423ND56NHO")
                .reversalRefNum("REV10001")
                .remark("Customer cancelled order")
                .build());

        assertEquals(PaymentStatus.REVERSED, response.paymentStatus());
        assertEquals("REV10001", response.prestoReversalRefNum());
    }

    @Test
    void refundRoundTrip() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("prestoRefundRefNum", "RFD10001");
            response.put("amount", 1200);
            response.put("refundAmount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentStatus", "Refunded");
            response.put("refundedDate", "20250423105225.000");
            response.put("ts", "20250423105230.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentRefundResponse response = client.payments().refund(PaymentRefundRequest.builder()
                .paymentRefNum("PP250423ND56NHO")
                .refundRefNum("RFD10001")
                .remark("Customer requested refund")
                .build());

        assertEquals(1200, response.refundAmount());
        assertEquals(PaymentStatus.REFUNDED, response.paymentStatus());
    }

    @Test
    void multiLineRemarkSurvivesARealHttpRoundTripAndSignatureVerification() {
        String remark = "Customer requested refund.\r\nReason: item damaged on arrival.\nApproved by support.";

        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("prestoRefundRefNum", "RFD10001");
            response.put("amount", 1200);
            response.put("refundAmount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentStatus", "Refunded");
            response.put("refundedDate", "20250423105225.000");
            response.put("ts", "20250423105230.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        client.payments().refund(PaymentRefundRequest.builder()
                .paymentRefNum("PP250423ND56NHO")
                .refundRefNum("RFD10001")
                .remark(remark)
                .build());

        assertEquals(remark, JsonCodec.text(mockGateway.lastRequestBody(), "remark"));
        assertRequestIsCorrectlySigned(mockGateway.lastRequestBody());
    }

    @Test
    void tamperedResponseSignatureFailsVerification() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("prestoMrn", MRN);
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("txnRefNum", "TXN10001");
            response.put("paymentStatus", "Authorised");
            response.put("amount", 1200);
            response.put("currencyCode", "MYR");
            response.put("paymentRequestDate", "20250423104500.000");
            response.put("refundDetails", "[]");
            response.put("paymentDetails", "[]");
            response.put("ts", "20250423104530.000");
            response.put("success", true);

            String canonical = Canonicalizer.canonicalize(response);
            response.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));
            response.put("amount", 999999);
            return MockGatewayServer.Response.preSigned(200, response);
        });

        assertThrows(PrestoPaySignatureException.class, () -> client.payments().query(
                PaymentQueryRequest.builder().paymentRefNum("PP250423ND56NHO").build()));
    }

    @Test
    void systemErrorHasEmptyBodyAndHeaderDerivedErrorCode() {
        mockGateway.handler(request -> MockGatewayServer.Response.systemError(400, "1006", "Invalid request."));

        PrestoPayApiException exception = assertThrows(PrestoPayApiException.class, () -> client.payments().query(
                PaymentQueryRequest.builder().paymentRefNum("PP250423ND56NHO").build()));

        assertTrue(exception.isSystemError());
        assertEquals("1006", exception.errorCode());
        assertEquals(400, exception.httpStatus());
    }

    @Test
    void businessErrorMapsToApiExceptionWithParsedFields() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("success", false);
            response.put("ts", "20260922135741.041");
            response.put("errorCode", "1201");
            response.put("errorMessage", "Invalid input.");
            return MockGatewayServer.Response.signed(200, response);
        });

        PrestoPayApiException exception = assertThrows(PrestoPayApiException.class, () -> client.payments().init(
                PaymentInitRequest.builder()
                        .txnType(TxnType.QR_PAY)
                        .txnRefNum("TXN10001")
                        .displayDesc("Order #12345")
                        .build()));

        assertFalse(exception.isSystemError());
        assertEquals("1201", exception.errorCode());
        assertEquals("Invalid input.", exception.errorMessage());
    }

    @Test
    void unsignedWebhookBodyIsRejected() {
        WebhookVerifier verifier = WebhookVerifier.builder().prestoPublicKey(TestKeys.publicKey()).build();
        assertThrows(PrestoPaySignatureException.class, () -> verifier.parse("not json at all"));
    }

    @Test
    void validWebhookParsesIntoATypedEvent() {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", "Authorised");
        body.put("mid", MID);
        body.put("prestoMrn", MRN);
        body.put("paymentRefNum", "PP250423ND56NHO");
        body.put("txnRefNum", "TXN998877");
        body.put("success", true);
        body.put("eventRefNum", "REV12345");
        body.put("eventTs", "20250423093000.000");
        body.put("amount", 5000);
        body.put("currencyCode", "MYR");
        body.put("additionalData", "");
        body.put("paymentDetails", "[{\"method\":\"Wallet\",\"amount\":5000}]");
        body.put("ts", Timestamps.now(Clock.systemUTC()));
        String canonical = Canonicalizer.canonicalize(body);
        body.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));

        WebhookVerifier verifier = WebhookVerifier.builder().prestoPublicKey(TestKeys.publicKey()).build();
        NotifyEvent event = verifier.parse(JsonCodec.write(body));

        assertEquals("Authorised", event.eventCode().value());
        assertTrue(event.eventCode().isKnown());
        assertTrue(event.success());
        assertEquals(1, event.paymentDetails().size());
    }

    private void assertRequestIsCorrectlySigned(JsonObject sentRequest) {
        String signature = JsonCodec.text(sentRequest, "signature");
        JsonObject withoutSignature = sentRequest.deepCopy();
        withoutSignature.remove("signature");
        String canonical = Canonicalizer.canonicalize(withoutSignature);
        assertTrue(RsaSignatureService.verify(canonical, signature, TestKeys.publicKey()));
    }
}

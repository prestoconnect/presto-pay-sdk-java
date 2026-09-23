package com.prestouniverse.pay;

import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.internal.Canonicalization;
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
import com.prestouniverse.pay.webhooks.NotifyEventCode;
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
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .merchantId(MID)
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

        PaymentInitResponse response = client.payments().init(initRequest()
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

        PaymentInitResponse response = client.payments().init(initRequest()
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
                queryRequest().paymentRefNum("PP250423ND56NHO").build());

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
                queryRequest().paymentRefNum("PP250423ND56NHO").build());

        assertNull(response.paymentDetails().get(0).method());
    }

    @Test
    void queryPaymentDetailParsesRefNum() {
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
            response.put("paymentDetails", "[{\"method\":\"Wallet\",\"amount\":1200,\"refNum\":\"PD-REF-99\"}]");
            response.put("ts", "20250423105030.000");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentQueryResponse response = client.payments().query(
                queryRequest().paymentRefNum("PP250423ND56NHO").build());

        assertEquals("PD-REF-99", response.paymentDetails().get(0).refNum());
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

        PaymentReverseResponse response = client.payments().reverse(reverseRequest()
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

        PaymentRefundResponse response = client.payments().refund(refundRequest()
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

        client.payments().refund(refundRequest()
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

            String canonical = Canonicalization.canonicalize(response);
            response.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));
            response.put("amount", 999999);
            return MockGatewayServer.Response.preSigned(200, response);
        });

        assertThrows(PrestoPaySignatureException.class, () -> client.payments().query(
                queryRequest().paymentRefNum("PP250423ND56NHO").build()));
    }

    @Test
    void systemErrorHasEmptyBodyAndHeaderDerivedErrorCode() {
        mockGateway.handler(request -> MockGatewayServer.Response.systemError(400, "1006", "Invalid request."));

        PrestoPayApiException exception = assertThrows(PrestoPayApiException.class, () -> client.payments().query(
                queryRequest().paymentRefNum("PP250423ND56NHO").build()));

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
                initRequest()
                        .txnType(TxnType.QR_PAY)
                        .txnRefNum("TXN10001")
                        .displayDesc("Order #12345")
                        .build()));

        assertFalse(exception.isSystemError());
        assertEquals("1201", exception.errorCode());
        assertEquals("Invalid input.", exception.errorMessage());
    }

    @Test
    void initResponseWithoutNonEssentialFieldsStillReturnsThePaymentUrl() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("paymentStatus", "PendingAuthorise");
            response.put("paymentUrl", "https://hpp-staging.prestouniverse.com/PM240110XDSFC/PP250423ND56NHO");
            response.put("amount", 1200);
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentInitResponse response = client.payments().init(initRequest()
                .txnType(TxnType.QR_PAY)
                .txnRefNum("TXN10001")
                .displayDesc("Order #12345")
                .build());

        assertEquals("https://hpp-staging.prestouniverse.com/PM240110XDSFC/PP250423ND56NHO", response.paymentUrl());
        assertNull(response.currencyCode());
    }

    @Test
    void signedResponseMissingARequiredFieldThrowsAResponseException() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("paymentStatus", "Authorised");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PrestoPayResponseException exception = assertThrows(PrestoPayResponseException.class,
                () -> client.payments().query(queryRequest().paymentRefNum("PP1").build()));

        assertEquals(PrestoPayResponseException.Source.RESPONSE, exception.source());
        assertTrue(exception.rawBody().contains("Authorised"));
    }

    @Test
    void responseWithAnUncanonicalizableValueThrowsAResponseException() {
        String body = "{\"paymentRefNum\":\"PP1\",\"amount\":12.5,\"success\":true,\"signature\":\"AAAA\"}";
        mockGateway.handler(request -> MockGatewayServer.Response.raw(200, body));

        PrestoPayResponseException exception = assertThrows(PrestoPayResponseException.class,
                () -> client.payments().query(queryRequest().paymentRefNum("PP1").build()));

        assertEquals(body, exception.rawBody());
    }

    @Test
    void nonJsonSuccessResponseThrowsAResponseException() {
        mockGateway.handler(request -> MockGatewayServer.Response.raw(200, "<html>proxy error</html>"));

        assertThrows(PrestoPayResponseException.class,
                () -> client.payments().query(queryRequest().paymentRefNum("PP1").build()));
    }

    @Test
    void omittedAmountsAreNullRatherThanZero() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("paymentRefNum", "PP250423ND56NHO");
            response.put("paymentStatus", "Refunded");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        PaymentRefundResponse refund = client.payments().refund(refundRequest()
                .paymentRefNum("PP250423ND56NHO")
                .refundRefNum("RFD10001")
                .remark("Customer requested refund")
                .build());
        PaymentQueryResponse query = client.payments().query(
                queryRequest().paymentRefNum("PP250423ND56NHO").build());

        assertNull(refund.amount());
        assertNull(refund.refundAmount());
        assertNull(query.amount());
    }

    @Test
    void malformedWebhookBodyIsRejected() {
        WebhookVerifier verifier = verifier();

        PrestoPayResponseException exception = assertThrows(PrestoPayResponseException.class,
                () -> verifier.parse("not json at all"));

        assertEquals(PrestoPayResponseException.Source.WEBHOOK, exception.source());
    }

    @Test
    void webhookWithoutASignatureIsRejected() {
        assertThrows(PrestoPaySignatureException.class,
                () -> client.webhooks().parse("{\"mid\":\"" + MID + "\",\"eventCode\":\"Authorised\"}"));
    }

    @Test
    void validWebhookParsesIntoATypedEvent() {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", PaymentStatus.AUTHORISED);
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
        String canonical = Canonicalization.canonicalize(body);
        body.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));

        WebhookVerifier verifier = verifier();
        NotifyEvent event = verifier.parse(JsonCodec.write(body));

        assertEquals(NotifyEventCode.AUTHORISED, event.eventCode());
        assertEquals(PaymentStatus.AUTHORISED, event.eventCode());
        assertTrue(event.success());
        assertEquals(PaymentStatus.AUTHORISED, event.paymentStatus());
        assertEquals(1, event.paymentDetails().size());
    }

    @Test
    void clientWebhooksAcceptEventsForTheConfiguredMerchant() {
        NotifyEvent event = client.webhooks().parse(signedWebhook(MID, Timestamps.now(Clock.systemUTC())));

        assertEquals(MID, event.mid());
    }

    @Test
    void clientWebhooksRejectGenuineEventsForAnotherMerchant() {
        String body = signedWebhook("OTHER-MERCHANT", Timestamps.now(Clock.systemUTC()));

        PrestoPaySignatureException exception = assertThrows(PrestoPaySignatureException.class,
                () -> client.webhooks().parse(body));

        assertEquals(PrestoPaySignatureException.Side.WEBHOOK, exception.side());
    }

    @Test
    void clientSendsItsMidAndEachRequestsOwnPrestoMrn() {
        mockGateway.handler(request -> {
            JsonObject response = JsonCodec.newObject();
            response.put("paymentRefNum", "PP1");
            response.put("success", true);
            return MockGatewayServer.Response.signed(200, response);
        });

        client.payments().query(PaymentQueryRequest.builder().merchantRefNum("MRN-1").paymentRefNum("PP1").build());
        assertEquals(MID, JsonCodec.text(mockGateway.lastRequestBody(), "mid"));
        assertEquals("MRN-1", JsonCodec.text(mockGateway.lastRequestBody(), "prestoMrn"));

        client.payments().query(PaymentQueryRequest.builder().merchantRefNum("MRN-2").paymentRefNum("PP1").build());
        assertEquals(MID, JsonCodec.text(mockGateway.lastRequestBody(), "mid"));
        assertEquals("MRN-2", JsonCodec.text(mockGateway.lastRequestBody(), "prestoMrn"));
        assertEquals(MID, client.merchantId());
    }

    @Test
    void clientAndWebhookVerifierRequireAMerchantId() {
        assertEquals("merchantId", assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .baseUrl(mockGateway.baseUrl())
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .build()).field());
        assertEquals("merchantId", assertThrows(PrestoPayConfigException.class, () -> WebhookVerifier.builder()
                .prestoPublicKey(TestKeys.publicKey())
                .merchantId(" ")
                .build()).field());
    }

    @Test
    void signedWebhookMissingARequiredFieldThrowsAResponseException() {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", NotifyEventCode.AUTHORISED);
        body.put("mid", MID);
        String canonical = Canonicalization.canonicalize(body);
        body.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));

        assertThrows(PrestoPayResponseException.class, () -> client.webhooks().parse(JsonCodec.write(body)));
    }

    @Test
    void signedWebhookWithAnUnparseableTimestampThrowsAResponseException() {
        WebhookVerifier verifier = WebhookVerifier.builder()
                .prestoPublicKey(TestKeys.publicKey())
                .merchantId(MID)
                .maxTimestampAge(Duration.ofMinutes(15))
                .build();

        assertThrows(PrestoPayResponseException.class, () -> verifier.parse(signedWebhook(MID, "not-a-ts")));
    }

    @Test
    void staleWebhookIsRejectedByDefault() {
        String stale = signedWebhook(MID, Timestamps.now(Clock.offset(Clock.systemUTC(), Duration.ofMinutes(-16))));

        assertThrows(PrestoPaySignatureException.class, () -> verifier().parse(stale));
        assertThrows(PrestoPaySignatureException.class, () -> client.webhooks().parse(stale));
    }

    @Test
    void replayWindowCanBeWidenedOnTheClientOrDisabledOnTheVerifier() {
        String stale = signedWebhook(MID, Timestamps.now(Clock.offset(Clock.systemUTC(), Duration.ofMinutes(-16))));
        PrestoPayClient tolerant = PrestoPayClient.builder()
                .baseUrl(mockGateway.baseUrl())
                .merchantId(MID)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .webhookMaxTimestampAge(Duration.ofMinutes(30))
                .build();
        WebhookVerifier unchecked = WebhookVerifier.builder()
                .prestoPublicKey(TestKeys.publicKey())
                .merchantId(MID)
                .disableTimestampCheck()
                .build();

        assertEquals(MID, tolerant.webhooks().parse(stale).mid());
        assertEquals(MID, unchecked.parse(stale).mid());
        assertEquals("maxTimestampAge", assertThrows(PrestoPayConfigException.class, () -> WebhookVerifier.builder()
                .prestoPublicKey(TestKeys.publicKey())
                .merchantId(MID)
                .maxTimestampAge(Duration.ZERO)
                .build()).field());
    }

    @Test
    void deeplyNestedWebhookBodyIsRejectedBeforeSignatureVerification() {
        StringBuilder body = new StringBuilder("{\"a\":");
        for (int i = 0; i < 100_000; i++) {
            body.append('[');
        }

        assertThrows(PrestoPayResponseException.class, () -> client.webhooks().parse(body.toString()));
    }

    private static WebhookVerifier verifier() {
        return WebhookVerifier.builder().prestoPublicKey(TestKeys.publicKey()).merchantId(MID).build();
    }

    private static PaymentInitRequest.Builder initRequest() {
        return PaymentInitRequest.builder().merchantRefNum(MRN);
    }

    private static PaymentQueryRequest.Builder queryRequest() {
        return PaymentQueryRequest.builder().merchantRefNum(MRN);
    }

    private static PaymentReverseRequest.Builder reverseRequest() {
        return PaymentReverseRequest.builder().merchantRefNum(MRN);
    }

    private static PaymentRefundRequest.Builder refundRequest() {
        return PaymentRefundRequest.builder().merchantRefNum(MRN);
    }

    private static String signedWebhook(String mid, String ts) {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", NotifyEventCode.AUTHORISED);
        body.put("mid", mid);
        body.put("prestoMrn", MRN);
        body.put("paymentRefNum", "PP250423ND56NHO");
        body.put("txnRefNum", "TXN998877");
        body.put("success", true);
        body.put("eventRefNum", "EV12345");
        body.put("eventTs", "20250423093000.000");
        body.put("amount", 5000);
        body.put("currencyCode", "MYR");
        body.put("paymentDetails", "[]");
        body.put("ts", ts);
        String canonical = Canonicalization.canonicalize(body);
        body.put("signature", RsaSignatureService.sign(canonical, TestKeys.privateKey()));
        return JsonCodec.write(body);
    }

    private void assertRequestIsCorrectlySigned(JsonObject sentRequest) {
        String signature = JsonCodec.text(sentRequest, "signature");
        JsonObject withoutSignature = sentRequest.deepCopy();
        withoutSignature.remove("signature");
        String canonical = Canonicalization.canonicalize(withoutSignature);
        assertTrue(RsaSignatureService.verify(canonical, signature, TestKeys.publicKey()));
    }
}

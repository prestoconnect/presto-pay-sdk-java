package com.prestouniverse.pay;

import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.internal.Canonicalization;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.support.MockGatewayServer;
import com.prestouniverse.pay.support.TestKeys;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrestoPayClientBuilderTest {

    private static final String MID = "PW2401XH9KCX";

    @Test
    void missingPrivateKeyIsRejected() {
        assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .merchantId(MID)
                .environment(Environment.STAGING)
                .prestoPublicKey(TestKeys.publicKey())
                .build());
    }

    @Test
    void missingBaseUrlAndEnvironmentIsRejected() {
        assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .merchantId(MID)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .build());
    }

    @Test
    void timeoutsThatHttpUrlConnectionWouldTreatAsInfiniteOrOverflowAreRejected() {
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().connectTimeout(Duration.ZERO).build());
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().readTimeout(Duration.ofNanos(1)).build());
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().readTimeout(Duration.ofSeconds(-1)).build());
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().connectTimeout(null).build());
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().readTimeout(Duration.ofDays(30)).build());
    }

    @Test
    void nullRetryPolicyAndClockAreRejected() {
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().retryPolicy(null).build());
        assertThrows(PrestoPayConfigException.class, () -> validBuilder().clock(null).build());
    }

    @Test
    void baseUrlTrailingSlashesAreIgnored() {
        try (MockGatewayServer mockGateway = MockGatewayServer.start()) {
            mockGateway.handler(PrestoPayClientBuilderTest::signedQueryResponse);
            PrestoPayClient client = PrestoPayClient.builder()
                    .merchantId(MID)
                    .baseUrl(mockGateway.baseUrl() + "//")
                    .privateKey(TestKeys.privateKey())
                    .prestoPublicKey(TestKeys.publicKey())
                    .build();

            client.payments().query(queryRequest());

            assertEquals("/v1/ext/payment/query", mockGateway.lastRequestPath());
        }
    }

    @Test
    void nonHttpBaseUrlIsRejected() {
        PrestoPayConfigException error = assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .merchantId(MID)
                .baseUrl("pay-ext.prestouniverse.com")
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .build());
        assertEquals("baseUrl", error.field());
    }

    @Test
    void fromEnvBuildsAWorkingClientWithoutAKeystoreAlias() throws URISyntaxException {
        try (MockGatewayServer mockGateway = MockGatewayServer.start()) {
            mockGateway.handler(PrestoPayClientBuilderTest::signedQueryResponse);

            Map<String, String> env = new HashMap<>();
            env.put("PRESTOPAY_BASE_URL", mockGateway.baseUrl());
            env.put("PRESTOPAY_MID", MID);
            env.put("PRESTOPAY_KEYSTORE_PATH", resourcePath("keys/test-keystore.p12").toString());
            env.put("PRESTOPAY_KEYSTORE_PASSWORD", TestKeys.KEYSTORE_PASSWORD);
            env.put("PRESTOPAY_PUBLIC_KEY_PATH", resourcePath("keys/test-publickey.der").toString());

            PrestoPayClient client = PrestoPayClient.fromEnv(env);

            PaymentQueryResponse response = client.payments().query(queryRequest());

            assertEquals("PP1", response.paymentRefNum());
            assertEquals(MID, JsonCodec.text(mockGateway.lastRequestBody(), "mid"));
        }
    }

    @Test
    void fromEnvRestrictsWebhooksToPrestoPayMid() throws URISyntaxException {
        Map<String, String> env = stagingEnv();
        env.put("PRESTOPAY_KEYSTORE_ALIAS", TestKeys.KEYSTORE_ALIAS);

        PrestoPayClient client = PrestoPayClient.fromEnv(env);

        assertEquals(MID, client.merchantId());
        assertEquals(MID, client.webhooks().parse(signedWebhook(MID)).mid());
        assertThrows(PrestoPaySignatureException.class, () -> client.webhooks().parse(signedWebhook("OTHER")));
    }

    @Test
    void fromEnvRequiresPrestoPayMid() throws URISyntaxException {
        Map<String, String> env = stagingEnv();
        env.remove("PRESTOPAY_MID");

        assertEquals("PRESTOPAY_MID",
                assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.fromEnv(env)).field());
    }

    private static Map<String, String> stagingEnv() throws URISyntaxException {
        Map<String, String> env = new HashMap<>();
        env.put("PRESTOPAY_ENV", "staging");
        env.put("PRESTOPAY_MID", MID);
        env.put("PRESTOPAY_KEYSTORE_PATH", resourcePath("keys/test-keystore.p12").toString());
        env.put("PRESTOPAY_KEYSTORE_PASSWORD", TestKeys.KEYSTORE_PASSWORD);
        env.put("PRESTOPAY_PUBLIC_KEY_PATH", resourcePath("keys/test-publickey.der").toString());
        return env;
    }

    private static PaymentQueryRequest queryRequest() {
        return PaymentQueryRequest.builder()
                .merchantRefNum("PM240110XDSFC")
                .paymentRefNum("PP1")
                .build();
    }

    private static MockGatewayServer.Response signedQueryResponse(JsonObject request) {
        JsonObject response = JsonCodec.newObject();
        response.put("prestoMrn", JsonCodec.text(request, "prestoMrn"));
        response.put("paymentRefNum", "PP1");
        response.put("txnRefNum", "TXN1");
        response.put("paymentStatus", "Authorised");
        response.put("amount", 1200);
        response.put("currencyCode", "MYR");
        response.put("paymentRequestDate", "20250423104500.000");
        response.put("refundDetails", "[]");
        response.put("paymentDetails", "[]");
        response.put("ts", "20250423105030.000");
        response.put("success", true);
        return MockGatewayServer.Response.signed(200, response);
    }

    private static String signedWebhook(String mid) {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", "Authorised");
        body.put("mid", mid);
        body.put("prestoMrn", "MRN");
        body.put("paymentRefNum", "PP1");
        body.put("txnRefNum", "TXN1");
        body.put("success", true);
        body.put("eventRefNum", "EV1");
        body.put("eventTs", "20250423093000.000");
        body.put("amount", 100);
        body.put("currencyCode", "MYR");
        body.put("ts", "20250423093000.000");
        body.put("signature", RsaSignatureService.sign(Canonicalization.canonicalize(body), TestKeys.privateKey()));
        return JsonCodec.write(body);
    }

    private static PrestoPayClient.Builder validBuilder() {
        return PrestoPayClient.builder()
                .merchantId(MID)
                .environment(Environment.STAGING)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey());
    }

    private static Path resourcePath(String name) throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Test resource not found: " + name);
        }
        return Paths.get(url.toURI());
    }
}

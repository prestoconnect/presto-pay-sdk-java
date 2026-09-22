package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentQueryParams;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.support.MockGatewayServer;
import com.prestouniverse.pay.support.TestKeys;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrestoPayClientBuilderTest {

    @Test
    void missingPrivateKeyIsRejected() {
        assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .environment(Environment.STAGING)
                .prestoPublicKey(TestKeys.publicKey())
                .build());
    }

    @Test
    void missingBaseUrlAndEnvironmentIsRejected() {
        assertThrows(PrestoPayConfigException.class, () -> PrestoPayClient.builder()
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .build());
    }

    @Test
    void requestWithNoMerchantIdAnywhereIsRejectedBeforeAnyNetworkCall() {
        PrestoPayClient client = PrestoPayClient.builder()
                .environment(Environment.STAGING)
                .privateKey(TestKeys.privateKey())
                .prestoPublicKey(TestKeys.publicKey())
                .build();

        assertThrows(PrestoPayConfigException.class, () -> client.payments().query(
                PaymentQueryParams.builder().paymentRefNum("PP1").build()));
    }

    @Test
    void fromEnvBuildsAWorkingClient() throws URISyntaxException {
        try (MockGatewayServer mockGateway = MockGatewayServer.start()) {
            mockGateway.handler(request -> {
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
            });

            Map<String, String> env = new HashMap<>();
            env.put("PRESTOPAY_BASE_URL", mockGateway.baseUrl());
            env.put("PRESTOPAY_MID", "PW2401XH9KCX");
            env.put("PRESTOPAY_MRN", "PM240110XDSFC");
            env.put("PRESTOPAY_KEYSTORE_PATH", resourcePath("keys/test-keystore.p12").toString());
            env.put("PRESTOPAY_KEYSTORE_PASSWORD", TestKeys.KEYSTORE_PASSWORD);
            env.put("PRESTOPAY_KEYSTORE_ALIAS", TestKeys.KEYSTORE_ALIAS);
            env.put("PRESTOPAY_PUBLIC_KEY_PATH", resourcePath("keys/test-publickey.der").toString());

            PrestoPayClient client = PrestoPayClient.fromEnv(env);

            PaymentQueryResponse response = client.payments().query(
                    PaymentQueryParams.builder().paymentRefNum("PP1").build());

            assertEquals("PP1", response.paymentRefNum());
        }
    }

    private static Path resourcePath(String name) throws URISyntaxException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url == null) {
            throw new IllegalStateException("Test resource not found: " + name);
        }
        return Paths.get(url.toURI());
    }
}

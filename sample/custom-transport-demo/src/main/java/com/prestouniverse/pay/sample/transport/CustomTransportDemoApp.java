package com.prestouniverse.pay.sample.transport;

import com.prestouniverse.pay.Environment;
import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.PrestoPayKeys;
import com.prestouniverse.pay.http.HttpTransport;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;

import java.nio.file.Paths;

/**
 * Wires one of {@link RestClientHttpTransport}, {@link JdkHttpClientTransport}, or {@link OkHttpTransport}
 * into a {@link PrestoPayClient} and issues a single {@code query} call, to demonstrate that any of the
 * three plug into {@code PrestoPayClient.Builder.transport(...)} exactly like {@link HttpTransport#jdkDefault()}.
 *
 * <p>This is reference code, not a runnable demo app on its own: it expects the same
 * {@code PRESTOPAY_*} environment variables as {@code PrestoPayClient.fromEnv()} (see the main README),
 * plus {@code PRESTOPAY_TRANSPORT} set to one of {@code rest-client}, {@code jdk-http-client}, or
 * {@code okhttp} (default: {@code jdk-http-client}), and {@code PRESTOPAY_QUERY_TXN_REF_NUM} naming an
 * existing {@code txnRefNum} to query.
 */
public final class CustomTransportDemoApp {

    private CustomTransportDemoApp() {
    }

    public static void main(String[] args) {
        HttpTransport transport = resolveTransport(System.getenv("PRESTOPAY_TRANSPORT"));

        PrestoPayClient client = PrestoPayClient.builder()
                .environment(parseEnvironment(requireEnv("PRESTOPAY_ENV")))
                .merchantId(requireEnv("PRESTOPAY_MID"))
                .privateKey(PrestoPayKeys.privateKeyFromPkcs12(
                        Paths.get(requireEnv("PRESTOPAY_KEYSTORE_PATH")),
                        requireEnv("PRESTOPAY_KEYSTORE_PASSWORD").toCharArray(),
                        System.getenv("PRESTOPAY_KEYSTORE_ALIAS")))
                .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get(requireEnv("PRESTOPAY_PUBLIC_KEY_PATH"))))
                .transport(transport)
                .build();

        PaymentQueryRequest request = PaymentQueryRequest.builder()
                .merchantRefNum(requireEnv("PRESTOPAY_MRN"))
                .txnRefNum(requireEnv("PRESTOPAY_QUERY_TXN_REF_NUM"))
                .build();

        PaymentQueryResponse response = client.payments().query(request);
        System.out.println("Queried with " + transport.getClass().getSimpleName()
                + ": paymentStatus=" + response.paymentStatus());
    }

    private static HttpTransport resolveTransport(String name) {
        if (name == null || name.isEmpty() || "jdk-http-client".equals(name)) {
            return new JdkHttpClientTransport();
        }
        if ("rest-client".equals(name)) {
            return new RestClientHttpTransport();
        }
        if ("okhttp".equals(name)) {
            return new OkHttpTransport();
        }
        throw new IllegalArgumentException(
                "Unknown PRESTOPAY_TRANSPORT: " + name + " (expected rest-client, jdk-http-client, or okhttp)");
    }

    private static Environment parseEnvironment(String name) {
        if ("staging".equalsIgnoreCase(name)) {
            return Environment.STAGING;
        }
        if ("production".equalsIgnoreCase(name)) {
            return Environment.PRODUCTION;
        }
        throw new IllegalArgumentException("Unknown PRESTOPAY_ENV: " + name);
    }

    private static String requireEnv(String key) {
        String value = System.getenv(key);
        if (value == null || value.isEmpty()) {
            throw new IllegalStateException(key + " environment variable is required");
        }
        return value;
    }
}

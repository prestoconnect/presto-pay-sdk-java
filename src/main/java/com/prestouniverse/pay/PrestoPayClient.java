package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.http.HttpTransport;
import com.prestouniverse.pay.internal.RequestPipeline;
import com.prestouniverse.pay.internal.SdkAccess;
import com.prestouniverse.pay.payments.PaymentsClient;
import com.prestouniverse.pay.webhooks.WebhookVerifier;
import com.prestouniverse.pay.webhooks.WebhooksClient;

import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;

/**
 * Thread-safe entry point for the Presto Connect payment gateway.
 *
 * <p>Build one client per merchant ({@code mid}), key pair, and environment, then reuse it across threads. The
 * client sends its {@link Builder#merchantId(String) merchantId} as {@code mid} on every request and
 * {@code webhooks()} only accepts events for that {@code mid}. Each {@code *Request} sets its own
 * {@code merchantRefNum} ({@code prestoMrn}). To serve several merchants, build one client per {@code mid}.
 *
 * <p>If {@code init} fails with a transport timeout after the request may have been sent, do not
 * retry {@code init} with the same {@code txnRefNum}. Call {@code query} with that {@code txnRefNum}
 * to reconcile state instead.
 *
 * @see com.prestouniverse.pay.payments.PaymentsClient
 * @see com.prestouniverse.pay.webhooks.WebhooksClient
 */
public final class PrestoPayClient {

    private final String merchantId;
    private final PaymentsClient payments;
    private final WebhooksClient webhooks;

    private PrestoPayClient(String merchantId, PaymentsClient payments, WebhooksClient webhooks) {
        this.merchantId = merchantId;
        this.payments = payments;
        this.webhooks = webhooks;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static PrestoPayClient fromEnv() {
        return fromEnv(System.getenv());
    }

    static PrestoPayClient fromEnv(Map<String, String> env) {
        Builder builder = builder();

        String baseUrl = env.get("PRESTOPAY_BASE_URL");
        String envName = env.get("PRESTOPAY_ENV");
        if (baseUrl != null) {
            builder.baseUrl(baseUrl);
        } else if (envName != null) {
            builder.environment(parseEnvironment(envName));
        } else {
            throw new PrestoPayConfigException("PRESTOPAY_ENV",
                    "Set PRESTOPAY_ENV=staging|production or PRESTOPAY_BASE_URL");
        }

        builder.merchantId(requireEnv(env, "PRESTOPAY_MID"));

        String keystorePath = requireEnv(env, "PRESTOPAY_KEYSTORE_PATH");
        String keystorePassword = requireEnv(env, "PRESTOPAY_KEYSTORE_PASSWORD");
        String keystoreAlias = env.get("PRESTOPAY_KEYSTORE_ALIAS");
        if (keystoreAlias != null && keystoreAlias.isEmpty()) {
            keystoreAlias = null;
        }
        String publicKeyPath = requireEnv(env, "PRESTOPAY_PUBLIC_KEY_PATH");

        builder.privateKey(PrestoPayKeys.privateKeyFromPkcs12(
                Paths.get(keystorePath), keystorePassword.toCharArray(), keystoreAlias));
        builder.prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get(publicKeyPath)));

        return builder.build();
    }

    private static String requireEnv(Map<String, String> env, String key) {
        String value = env.get(key);
        if (value == null || value.isEmpty()) {
            throw new PrestoPayConfigException(key, key + " environment variable is required");
        }
        return value;
    }

    private static Environment parseEnvironment(String name) {
        if ("staging".equalsIgnoreCase(name)) {
            return Environment.STAGING;
        }
        if ("production".equalsIgnoreCase(name)) {
            return Environment.PRODUCTION;
        }
        throw new PrestoPayConfigException("PRESTOPAY_ENV", "Unknown environment: " + name);
    }

    /** The {@code mid} this client sends on every request and accepts on webhooks. */
    public String merchantId() {
        return merchantId;
    }

    public PaymentsClient payments() {
        return payments;
    }

    public WebhooksClient webhooks() {
        return webhooks;
    }

    public static final class Builder {

        private Environment environment;
        private String baseUrl;
        private PrivateKey privateKey;
        private PublicKey prestoPublicKey;
        private HttpTransport transport;
        private Duration connectTimeout = Duration.ofSeconds(5);
        private Duration readTimeout = Duration.ofSeconds(30);
        private RetryPolicy retryPolicy = RetryPolicy.defaults();
        private Clock clock = Clock.systemUTC();
        private String merchantId;

        private Builder() {
        }

        public Builder environment(Environment environment) {
            this.environment = environment;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder privateKey(PrivateKey privateKey) {
            this.privateKey = privateKey;
            return this;
        }

        public Builder prestoPublicKey(PublicKey prestoPublicKey) {
            this.prestoPublicKey = prestoPublicKey;
            return this;
        }

        public Builder transport(HttpTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder retryPolicy(RetryPolicy retryPolicy) {
            this.retryPolicy = retryPolicy;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        /**
         * Required. Sent as {@code mid} on every request; {@code webhooks().parse} rejects events for any other
         * {@code mid}.
         */
        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public PrestoPayClient build() {
            String resolvedBaseUrl = resolveBaseUrl();
            if (merchantId == null || merchantId.trim().isEmpty()) {
                throw new PrestoPayConfigException("merchantId", "merchantId is required");
            }
            if (privateKey == null) {
                throw new PrestoPayConfigException("privateKey", "privateKey is required");
            }
            if (prestoPublicKey == null) {
                throw new PrestoPayConfigException("prestoPublicKey", "prestoPublicKey is required");
            }
            requireTimeout("connectTimeout", connectTimeout);
            requireTimeout("readTimeout", readTimeout);
            if (retryPolicy == null) {
                throw new PrestoPayConfigException("retryPolicy", "retryPolicy must not be null");
            }
            if (clock == null) {
                throw new PrestoPayConfigException("clock", "clock must not be null");
            }
            HttpTransport resolvedTransport = transport != null ? transport : HttpTransport.jdkDefault();

            RequestPipeline pipeline = new RequestPipeline(resolvedBaseUrl, merchantId, privateKey, prestoPublicKey,
                    resolvedTransport, connectTimeout, readTimeout, retryPolicy, clock);

            PaymentsClient payments = SdkAccess.payments().newPaymentsClient(pipeline);
            WebhookVerifier verifier = WebhookVerifier.builder()
                    .prestoPublicKey(prestoPublicKey)
                    .merchantId(merchantId)
                    .build();
            WebhooksClient webhooks = new WebhooksClient(verifier);

            return new PrestoPayClient(merchantId, payments, webhooks);
        }

        private static void requireTimeout(String field, Duration timeout) {
            if (timeout == null || timeout.compareTo(Duration.ofMillis(1)) < 0) {
                throw new PrestoPayConfigException(field, field + " must be at least 1 ms");
            }
            if (timeout.compareTo(Duration.ofMillis(Integer.MAX_VALUE)) > 0) {
                throw new PrestoPayConfigException(field, field + " must be at most " + Integer.MAX_VALUE + " ms");
            }
        }

        private String resolveBaseUrl() {
            if (baseUrl != null) {
                String normalized = baseUrl.trim();
                while (normalized.endsWith("/")) {
                    normalized = normalized.substring(0, normalized.length() - 1);
                }
                String lower = normalized.toLowerCase(Locale.ROOT);
                if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
                    throw new PrestoPayConfigException("baseUrl", "baseUrl must start with https:// or http://");
                }
                return normalized;
            }
            if (environment != null) {
                return environment.baseUrl();
            }
            throw new PrestoPayConfigException("environment",
                    "Either environment(...) or baseUrl(...) is required");
        }
    }
}

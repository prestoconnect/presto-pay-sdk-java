package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.RetryPolicy;
import com.prestouniverse.pay.SdkVersion;
import com.prestouniverse.pay.crypto.Canonicalizer;
import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException.Side;
import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

public final class RequestPipeline {

    private static final String USER_AGENT =
            "presto-pay-sdk/" + SdkVersion.version() + " java/" + System.getProperty("java.version", "unknown");

    private final String baseUrl;
    private final String defaultMerchantId;
    private final String defaultMerchantRefNum;
    private final PrivateKey privateKey;
    private final PublicKey prestoPublicKey;
    private final HttpTransport transport;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public RequestPipeline(String baseUrl, String defaultMerchantId, String defaultMerchantRefNum,
            PrivateKey privateKey, PublicKey prestoPublicKey, HttpTransport transport, Duration connectTimeout,
            Duration readTimeout, RetryPolicy retryPolicy, Clock clock) {
        this.baseUrl = baseUrl;
        this.defaultMerchantId = defaultMerchantId;
        this.defaultMerchantRefNum = defaultMerchantRefNum;
        this.privateKey = privateKey;
        this.prestoPublicKey = prestoPublicKey;
        this.transport = transport;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        this.retryPolicy = retryPolicy;
        this.clock = clock;
    }

    public JsonObject execute(String path, JsonObject body, String merchantIdOverride,
            String merchantRefNumOverride, boolean idempotent) {
        body.put("mid", resolve("mid", merchantIdOverride, defaultMerchantId));
        body.put("prestoMrn", resolve("prestoMrn", merchantRefNumOverride, defaultMerchantRefNum));

        int attempt = 0;
        while (true) {
            attempt++;
            try {
                return attempt(path, body);
            } catch (PrestoPayTransportException e) {
                if (!canRetry(attempt, idempotent, e.requestNotSent())) {
                    throw e;
                }
                sleep(retryPolicy.backoffFor(attempt));
            } catch (PrestoPayApiException e) {
                if (!canRetryApiFailure(attempt, idempotent, e)) {
                    throw e;
                }
                sleep(retryPolicy.backoffFor(attempt));
            }
        }
    }

    private boolean canRetry(int attempt, boolean idempotent, boolean requestNotSent) {
        return attempt <= retryPolicy.maxRetries() && (idempotent || requestNotSent);
    }

    private boolean canRetryApiFailure(int attempt, boolean idempotent, PrestoPayApiException error) {
        return idempotent && error.isSystemError() && error.httpStatus() >= 500
                && attempt <= retryPolicy.maxRetries();
    }

    private JsonObject attempt(String path, JsonObject body) {
        JsonObject requestBody = body.deepCopy();
        requestBody.put("ts", Timestamps.now(clock));

        String canonical;
        try {
            canonical = Canonicalizer.canonicalize(requestBody);
        } catch (IllegalArgumentException e) {
            throw new PrestoPaySignatureException(Side.REQUEST, "Failed to canonicalize request body", null, e);
        }
        requestBody.put("signature", RsaSignatureService.sign(canonical, privateKey));

        HttpRequest httpRequest = HttpRequest.post(baseUrl + path, headers(), JsonCodec.write(requestBody));
        HttpResponse httpResponse = transport.execute(httpRequest, connectTimeout, readTimeout);
        return handleResponse(httpResponse);
    }

    private JsonObject handleResponse(HttpResponse response) {
        if (response.status() != 200) {
            String errorCode = response.header("x-http-error-code");
            String errorMessage = response.header("x-http-error");
            String body = response.body() != null ? response.body() : "";
            throw new PrestoPayApiException(response.status(), errorCode, errorMessage, true, body);
        }

        JsonObject node;
        try {
            node = JsonCodec.parseObject(response.body());
        } catch (IllegalArgumentException e) {
            throw new PrestoPaySignatureException(Side.RESPONSE, "Malformed response body", null, e);
        }

        String signature = JsonCodec.text(node, "signature");
        if (signature == null) {
            throw new PrestoPaySignatureException(Side.RESPONSE, "Response is missing a signature field", null);
        }
        String canonical = Canonicalizer.canonicalize(node);
        if (!RsaSignatureService.verify(canonical, signature, prestoPublicKey)) {
            throw new PrestoPaySignatureException(Side.RESPONSE, "Response signature verification failed",
                    canonical);
        }

        if (!JsonCodec.optBoolean(node, "success", true)) {
            String errorCode = JsonCodec.text(node, "errorCode");
            String errorMessage = JsonCodec.text(node, "errorMessage");
            throw new PrestoPayApiException(response.status(), errorCode, errorMessage, false, response.body());
        }
        return node;
    }

    private String resolve(String field, String override, String defaultValue) {
        if (override != null) {
            return override;
        }
        if (defaultValue != null) {
            return defaultValue;
        }
        throw new PrestoPayConfigException(field,
                field + " is required: set it on the client builder or on this request");
    }

    private Map<String, String> headers() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json; charset=UTF-8");
        headers.put("User-Agent", USER_AGENT);
        return headers;
    }

    private void sleep(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrestoPayTransportException("Interrupted while waiting to retry", e, false);
        }
    }
}

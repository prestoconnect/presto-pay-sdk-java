package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPayResponseException.Source;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException.Side;
import com.prestouniverse.pay.internal.Canonicalization;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.Timestamps;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.security.PublicKey;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

public final class WebhookVerifier {

    private final PublicKey prestoPublicKey;
    private final String merchantId;
    private final Duration maxTimestampAge;
    private final Clock clock;

    private WebhookVerifier(Builder builder) {
        this.prestoPublicKey = builder.prestoPublicKey;
        this.merchantId = builder.merchantId;
        this.maxTimestampAge = builder.maxTimestampAge;
        this.clock = builder.clock;
    }

    public static Builder builder() {
        return new Builder();
    }

    public NotifyEvent parse(String rawBody) {
        JsonObject node;
        try {
            node = JsonCodec.parseObject(rawBody);
        } catch (IllegalArgumentException e) {
            throw new PrestoPayResponseException(Source.WEBHOOK, "Malformed webhook body: " + e.getMessage(),
                    rawBody, e);
        }

        String signature = JsonCodec.text(node, "signature");
        if (signature == null) {
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Webhook body is missing a signature field", null);
        }

        String canonical;
        try {
            canonical = Canonicalization.canonicalize(node);
        } catch (IllegalArgumentException e) {
            throw new PrestoPayResponseException(Source.WEBHOOK,
                    "Failed to canonicalize webhook body: " + e.getMessage(), rawBody, e);
        }

        if (!RsaSignatureService.verify(canonical, signature, prestoPublicKey)) {
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Webhook signature verification failed",
                    canonical);
        }

        NotifyEvent event;
        try {
            event = NotifyEvent.fromJson(node);
        } catch (IllegalArgumentException e) {
            throw new PrestoPayResponseException(Source.WEBHOOK, "Invalid webhook body: " + e.getMessage(),
                    rawBody, e);
        }
        checkMerchant(event, canonical);
        checkFreshness(event, rawBody, canonical);
        return event;
    }

    private void checkMerchant(NotifyEvent event, String canonical) {
        if (!merchantId.equals(event.mid())) {
            throw new PrestoPaySignatureException(Side.WEBHOOK,
                    "Webhook mid '" + event.mid() + "' does not match merchantId '" + merchantId + "'", canonical);
        }
    }

    private void checkFreshness(NotifyEvent event, String rawBody, String canonical) {
        if (maxTimestampAge == null) {
            return;
        }
        Instant eventTime;
        try {
            eventTime = Timestamps.parse(event.ts());
        } catch (DateTimeException e) {
            throw new PrestoPayResponseException(Source.WEBHOOK, "Invalid webhook timestamp: " + event.ts(),
                    rawBody, e);
        }
        Duration age = Duration.between(eventTime, clock.instant()).abs();
        if (age.compareTo(maxTimestampAge) > 0) {
            throw new PrestoPaySignatureException(Side.WEBHOOK,
                    "Webhook timestamp is outside the allowed freshness window", canonical);
        }
    }

    public static final class Builder {

        private PublicKey prestoPublicKey;
        private String merchantId;
        private Duration maxTimestampAge;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder prestoPublicKey(PublicKey prestoPublicKey) {
            this.prestoPublicKey = prestoPublicKey;
            return this;
        }

        /**
         * Required. Webhooks whose {@code mid} differs are rejected: Presto signs webhooks for all partners with
         * the same key, so a genuine event for another merchant would otherwise verify.
         */
        public Builder merchantId(String merchantId) {
            this.merchantId = merchantId;
            return this;
        }

        public Builder maxTimestampAge(Duration maxTimestampAge) {
            this.maxTimestampAge = maxTimestampAge;
            return this;
        }

        public Builder clock(Clock clock) {
            this.clock = clock;
            return this;
        }

        public WebhookVerifier build() {
            if (prestoPublicKey == null) {
                throw new PrestoPayConfigException("prestoPublicKey", "prestoPublicKey is required");
            }
            if (merchantId == null || merchantId.trim().isEmpty()) {
                throw new PrestoPayConfigException("merchantId", "merchantId is required");
            }
            return new WebhookVerifier(this);
        }
    }
}

package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.crypto.Canonicalizer;
import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException.Side;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.Timestamps;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.security.PublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public final class WebhookVerifier {

    private final PublicKey prestoPublicKey;
    private final Duration maxTimestampAge;
    private final Clock clock;

    private WebhookVerifier(Builder builder) {
        this.prestoPublicKey = builder.prestoPublicKey;
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
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Malformed webhook body", null, e);
        }

        String signature = JsonCodec.text(node, "signature");
        if (signature == null) {
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Webhook body is missing a signature field", null);
        }

        String canonical;
        try {
            canonical = Canonicalizer.canonicalize(node);
        } catch (IllegalArgumentException e) {
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Failed to canonicalize webhook body", null, e);
        }

        if (!RsaSignatureService.verify(canonical, signature, prestoPublicKey)) {
            throw new PrestoPaySignatureException(Side.WEBHOOK, "Webhook signature verification failed",
                    canonical);
        }

        NotifyEvent event = NotifyEvent.fromJson(node);
        checkFreshness(event, canonical);
        return event;
    }

    private void checkFreshness(NotifyEvent event, String canonical) {
        if (maxTimestampAge == null) {
            return;
        }
        Instant eventTime = Timestamps.parse(event.ts());
        Duration age = Duration.between(eventTime, clock.instant()).abs();
        if (age.compareTo(maxTimestampAge) > 0) {
            throw new PrestoPaySignatureException(Side.WEBHOOK,
                    "Webhook timestamp is outside the allowed freshness window", canonical);
        }
    }

    public static final class Builder {

        private PublicKey prestoPublicKey;
        private Duration maxTimestampAge;
        private Clock clock = Clock.systemUTC();

        private Builder() {
        }

        public Builder prestoPublicKey(PublicKey prestoPublicKey) {
            this.prestoPublicKey = prestoPublicKey;
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
            return new WebhookVerifier(this);
        }
    }
}

package com.prestouniverse.pay.webhooks;

/** Verifies webhooks for the client's merchant. Obtain from {@code PrestoPayClient.webhooks()}; thread-safe. */
public final class WebhooksClient {

    private final WebhookVerifier verifier;

    public WebhooksClient(WebhookVerifier verifier) {
        this.verifier = verifier;
    }

    public NotifyEvent parse(String rawBody) {
        return verifier.parse(rawBody);
    }
}

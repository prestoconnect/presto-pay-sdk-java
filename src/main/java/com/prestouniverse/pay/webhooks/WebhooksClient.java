package com.prestouniverse.pay.webhooks;

public final class WebhooksClient {

    private final WebhookVerifier verifier;

    public WebhooksClient(WebhookVerifier verifier) {
        this.verifier = verifier;
    }

    public NotifyEvent parse(String rawBody) {
        return verifier.parse(rawBody);
    }
}

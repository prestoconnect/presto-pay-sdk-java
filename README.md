# Presto Pay SDK

[![Maven Central](https://img.shields.io/maven-central/v/com.prestouniverse/presto-pay-sdk.svg)](https://central.sonatype.com/artifact/com.prestouniverse/presto-pay-sdk)
[![CI](https://github.com/prestoconnect/presto-pay-sdk-java/actions/workflows/ci.yml/badge.svg)](https://github.com/prestoconnect/presto-pay-sdk-java/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Standalone, framework-agnostic Java library for the **Presto Connect** payment gateway. It handles the parts
that are easy to get subtly wrong when integrating a signed payment API by hand: typed request/response
models, RSA request signing, response and webhook signature verification, and PKCS#12 / X.509 key loading.

- **Java 8+** — no required framework or dependency injection container
- **Zero runtime dependencies** (JUnit is test-only)
- **Thread-safe** `PrestoPayClient` — build once, share across threads

## Contents

- [Install](#install)
- [Quick start](#quick-start)
- [Merchant identity](#merchant-identity)
- [Configuration from environment](#configuration-from-environment)
- [Retries and idempotency](#retries-and-idempotency)
- [Webhooks](#webhooks)
- [Errors](#errors)
- [Custom HTTP client](#custom-http-client)
- [Debugging signatures](#debugging-signatures)
- [Samples](#samples)
- [Contributing](#contributing)
- [License](#license)

## Install

Maven:

```xml
<dependency>
  <groupId>com.prestouniverse</groupId>
  <artifactId>presto-pay-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

Gradle: `implementation("com.prestouniverse:presto-pay-sdk:0.1.0")`

## Quick start

```java
PrestoPayClient client = PrestoPayClient.builder()
    .environment(Environment.STAGING)
    .merchantId("YOUR_MID")            // mid, sent on every request and required on webhooks
    .privateKey(PrestoPayKeys.privateKeyFromPkcs12(
        Paths.get("/path/to/partner.p12"), passwordChars)) // or pass an alias if the keystore holds several keys
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get("/path/to/presto.der")))
    .build();

PaymentInitResponse init = client.payments().init(PaymentInitRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN") // prestoMrn, required on every request
    .txnType(TxnType.WebPay)          // or any gateway string
    .txnRefNum("order-123")
    .displayDesc("Order 123")
    .amount(10_000)
    .currencyCode("MYR")
    .notifyUrl("https://your-app.example/presto/notify")
    .redirectUrl("https://your-app.example/presto/return")
    .build());

String paymentUrl = init.paymentUrl();
```

## Merchant identity

A client belongs to one merchant: `merchantId` (`mid`) is required on the builder, sent on every request, and `client.webhooks()` rejects events for any other `mid`. `merchantRefNum` (`prestoMrn`) is set per request, so one client can use several `prestoMrn`s under its `mid`; each `*Request.build()` throws `PrestoPayConfigException` if it is missing or blank.

To serve several merchants, build one client per `mid` (they can share the same keys) and route each request and webhook to the matching client, for example with a `Map<String, PrestoPayClient>` keyed by `mid`.

## Configuration from environment

```java
PrestoPayClient client = PrestoPayClient.fromEnv();
```

| Variable | Required | Description |
|----------|----------|-------------|
| `PRESTOPAY_ENV` | One of env or base URL | `staging` or `production` |
| `PRESTOPAY_BASE_URL` | Alternative to `PRESTOPAY_ENV` | Override gateway base URL |
| `PRESTOPAY_MID` | Yes | Merchant `mid` for this client |
| `PRESTOPAY_KEYSTORE_PATH` | Yes | PKCS#12 path |
| `PRESTOPAY_KEYSTORE_PASSWORD` | Yes | Keystore password |
| `PRESTOPAY_KEYSTORE_ALIAS` | No | Private key alias; required only if the keystore holds more than one private key |
| `PRESTOPAY_PUBLIC_KEY_PATH` | Yes | Presto X.509 certificate (DER or PEM) |

For production, prefer the builder with keys from your secret store and `char[]` passwords instead of environment strings when possible.

## Retries and idempotency

`init`, `reverse`, and `refund` are **not** safely retried after the HTTP request may have reached Presto. The default retry policy only retries those operations when `PrestoPayTransportException.requestNotSent()` is true.

If `init` times out or fails ambiguously **after** send, **do not** call `init` again with the same `txnRefNum` (duplicate refs return error `1203`). Reconcile with:

```java
PaymentQueryResponse status = client.payments().query(
    PaymentQueryRequest.builder()
        .merchantRefNum("YOUR_PRESTO_MRN")
        .txnRefNum("order-123")
        .build());
```

`query` is read-only and may be retried on transport errors and 5xx responses.

## Webhooks

Presto POSTs JSON to your `notifyUrl` from its infrastructure — the URL must be **publicly reachable** (not `localhost` unless you tunnel). Acknowledge with HTTP **200** and body `{"resend":false}` before heavy work:

```java
WebhookVerifier verifier = WebhookVerifier.builder()
    .prestoPublicKey(prestoPublicKey)
    .merchantId("YOUR_MID")                   // required; rejects events signed for other merchants
    .build();                                 // rejects events whose ts is more than 15 minutes off

NotifyEvent event = verifier.parse(rawRequestBody);
String suggestedStatus = event.paymentStatus(); // Authorised uses success; others map to PaymentStatus
// return 200 with NotifyAck.ok()
```

If you already have a `PrestoPayClient`, `client.webhooks().parse(rawBody)` uses the same Presto public key and only accepts events for the client's `merchantId`. Presto signs webhooks for every partner with the same key, so without this check a genuine event for another merchant would verify.

Both reject a webhook whose signed `ts` is more than 15 minutes from the local clock, so a captured webhook cannot be replayed later. Keep the host clock in sync (NTP). Adjust the window with `WebhookVerifier.Builder.maxTimestampAge(...)` or `PrestoPayClient.Builder.webhookMaxTimestampAge(...)`; `WebhookVerifier.Builder.disableTimestampCheck()` turns it off, but then you must deduplicate events yourself (for example by `eventRefNum`).

Reject webhooks that throw `PrestoPaySignatureException` (for example HTTP 401) or `PrestoPayResponseException` (for example HTTP 400) rather than letting them surface as a 500.

`event.paymentStatus()` reflects the notify contract (for **Authorised**, `success` indicates authorisation outcome). Compare `event.eventCode()` to `NotifyEventCode` constants. After any webhook, call `payments().query()` for authoritative payment status.

## Errors

All SDK errors extend `PrestoPayException` (unchecked):

| Type | When |
|------|------|
| `PrestoPayApiException` | HTTP 400 system errors (`x-http-error-code` headers) or HTTP 200 with `success: false` |
| `PrestoPaySignatureException` | Invalid or missing signature on responses/webhooks; webhook `mid` not allowed or timestamp outside the replay window |
| `PrestoPayResponseException` | Response or webhook body cannot be parsed (malformed JSON, missing required fields, bad formats); see `source()` and `rawBody()`. On `init`/`reverse`/`refund` the operation may have succeeded — reconcile with `query` |
| `PrestoPayTransportException` | Network, timeout, TLS; check `requestNotSent()` |
| `PrestoPayConfigException` | Builder validation (including timeouts under 1 ms), bad keys, missing env |

Compare business error codes to constants in `ErrorCode` (e.g. `1005` clock skew — ensure host time is accurate; timestamps are UTC+8).

## Custom HTTP client

Implement `HttpTransport` and pass it to the client builder (for proxies, pooling, or observability). The default uses JDK `HttpURLConnection`; wrap `HttpTransport.jdkDefault()` to decorate it, for example to add logging:

```java
HttpTransport jdk = HttpTransport.jdkDefault();
HttpTransport logging = (request, connectTimeout, readTimeout) -> {
    HttpResponse response = jdk.execute(request, connectTimeout, readTimeout);
    log.info("{} -> {}", request.url(), response.status());
    return response;
};
```

A custom transport must return non-2xx responses rather than throw, must not follow redirects or resend requests, and must throw `PrestoPayTransportException` with `requestNotSent = true` only when the request certainly never left the process. The SDK's retry safety for `init`, `reverse`, and `refund` depends on that flag being accurate.

For full reference implementations backed by Spring's `RestClient`, the JDK 11+ `HttpClient`, and OkHttp, see [sample/custom-transport](sample/custom-transport).

## Debugging signatures

Use `Canonicalizer.canonicalizeJson(jsonString)` to reproduce the gateway canonical string from raw JSON. Avoid depending on types under `com.prestouniverse.pay.internal` — they are not semver-stable.

## Samples

- [sample/my-store/](sample/my-store/) — Spring Boot 2.7 sample (Java 8+): a MyStore-branded checkout page with a toggle between hosted and self-hosted payment method selection.
- [sample/custom-transport/](sample/custom-transport/) — reference `HttpTransport` implementations backed by Spring's `RestClient`, the JDK 11+ `HttpClient`, and OkHttp.

See [sample/README.md](sample/README.md) for details on both.

## Contributing

Building, testing, code style, and the release process live in [CONTRIBUTING.md](CONTRIBUTING.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

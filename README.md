# Presto Pay SDK

Standalone, framework-agnostic Java library for [Presto Connect](https://github.com/prestouniverse/presto-pay-sdk) payment APIs: typed requests and responses, RSA request signing, response and webhook verification, and PKCS#12 / X.509 key loading.

- **Java 8+**
- **Zero runtime dependencies** (JUnit is test-only)
- **Thread-safe** `PrestoPayClient` — build once, share across threads

Maven coordinates (when published):

```xml
<dependency>
  <groupId>com.prestouniverse</groupId>
  <artifactId>presto-pay-sdk</artifactId>
  <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Until the artifact is on Maven Central, install locally with `mvn install`.

## Quick start

```java
PrestoPayClient client = PrestoPayClient.builder()
    .environment(Environment.STAGING)
    .merchantId("YOUR_MID")
    .merchantRefNum("YOUR_DEFAULT_PRESTO_MRN")
    .privateKey(PrestoPayKeys.privateKeyFromPkcs12(
        Paths.get("/path/to/partner.p12"), passwordChars, "alias"))
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get("/path/to/presto.der")))
    .build();

PaymentInitResponse init = client.payments().init(PaymentInitRequest.builder()
    .txnType(TxnType.WEB_PAY)  // or any gateway string
    .txnRefNum("order-123")
    .displayDesc("Order 123")
    .amount(10_000)
    .currencyCode("MYR")
    .notifyUrl("https://your-app.example/presto/notify")
    .redirectUrl("https://your-app.example/presto/return")
    .build());

String paymentUrl = init.paymentUrl();
```

Use a different sub-merchant per request with `.merchantRefNum(prestoMrn)` on the params builder (overrides the client default).

## Configuration from environment

```java
PrestoPayClient client = PrestoPayClient.fromEnv();
```

| Variable | Required | Description |
|----------|----------|-------------|
| `PRESTOPAY_ENV` | One of env or base URL | `staging` or `production` |
| `PRESTOPAY_BASE_URL` | Alternative to `PRESTOPAY_ENV` | Override gateway base URL |
| `PRESTOPAY_MID` | No | Default master merchant id |
| `PRESTOPAY_MRN` | No | Default sub-merchant ref |
| `PRESTOPAY_KEYSTORE_PATH` | Yes | PKCS#12 path |
| `PRESTOPAY_KEYSTORE_PASSWORD` | Yes | Keystore password |
| `PRESTOPAY_KEYSTORE_ALIAS` | Yes | Private key alias |
| `PRESTOPAY_PUBLIC_KEY_PATH` | Yes | Presto X.509 certificate (DER or PEM) |

For production, prefer the builder with keys from your secret store and `char[]` passwords instead of environment strings when possible.

## Init timeout and idempotency

`init`, `reverse`, and `refund` are **not** safely retried after the HTTP request may have reached Presto. The default retry policy only retries those operations when `PrestoPayTransportException.requestNotSent()` is true.

If `init` times out or fails ambiguously **after** send, **do not** call `init` again with the same `txnRefNum` (duplicate refs return error `1203`). Reconcile with:

```java
PaymentQueryResponse status = client.payments().query(
    PaymentQueryRequest.builder().txnRefNum("order-123").build());
```

`query` is read-only and may be retried on transport errors and 5xx responses.

## Webhooks

Presto POSTs JSON to your `notifyUrl` from its infrastructure — the URL must be **publicly reachable** (not `localhost` unless you tunnel). Acknowledge with HTTP **200** and body `{"resend":false}` before heavy work:

```java
WebhookVerifier verifier = WebhookVerifier.builder()
    .prestoPublicKey(prestoPublicKey)
    .maxTimestampAge(Duration.ofMinutes(15))  // optional replay window
    .build();

NotifyEvent event = verifier.parse(rawRequestBody);
String suggestedStatus = event.getPaymentStatus(); // Authorised uses success; others map to PaymentStatus
// return 200 with NotifyAck.ok()
```

If you already have a `PrestoPayClient`, `client.webhooks().parse(rawBody)` uses the same Presto public key.

`event.getPaymentStatus()` reflects the notify contract (for **Authorised**, `success` indicates authorisation outcome). Compare `event.eventCode()` to `NotifyEventCode` constants. After any webhook, call `payments().query()` for authoritative payment status.

## Errors

All SDK errors extend `PrestoPayException` (unchecked):

| Type | When |
|------|------|
| `PrestoPayApiException` | HTTP 400 system errors (`x-http-error-code` headers) or HTTP 200 with `success: false` |
| `PrestoPaySignatureException` | Invalid or missing signature on responses/webhooks |
| `PrestoPayTransportException` | Network, timeout, TLS; check `requestNotSent()` |
| `PrestoPayConfigException` | Builder validation, bad keys, missing env |

Compare business error codes to constants in `ErrorCode` (e.g. `1005` clock skew — ensure host time is accurate; timestamps are UTC+8).

## Custom HTTP client

Implement `HttpTransport` and pass it to the client builder (for proxies, pooling, or observability). The default uses JDK `HttpURLConnection`.

## Debugging signatures

Use `Canonicalizer.canonicalizeJson(jsonString)` to reproduce the gateway canonical string from raw JSON. Avoid depending on types under `com.prestouniverse.pay.internal` — they are not semver-stable.

## Building and testing

```bash
mvn verify
```

Optional live staging smoke (requires real staging credentials):

```bash
export PRESTOPAY_STAGING_SMOKE=1
# plus PRESTOPAY_ENV, keystore, and public key variables
mvn verify -Pstaging-smoke
```

Release artifacts (sources + Javadoc): `mvn verify -Prelease`.

## Spring wiring

See [docs/spring-wiring.md](docs/spring-wiring.md) for a minimal `@Configuration` example.

## Sample app

[sample/spring-boot-payment-demo/](sample/spring-boot-payment-demo/) — Spring Boot 2.7 sample (Java 8+): **hosted** and **self-hosted** checkout UIs. See [sample/README.md](sample/README.md).

## License

Apache License 2.0 — see [LICENSE](LICENSE).

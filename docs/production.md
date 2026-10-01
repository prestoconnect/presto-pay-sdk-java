# Production

- [Configuration from environment](#configuration-from-environment)
- [Loading keys from a secret store](#loading-keys-from-a-secret-store)
- [Several merchants](#several-merchants)
- [Custom HTTP client](#custom-http-client)
- [Going live checklist](#going-live-checklist)
- [Troubleshooting](#troubleshooting)

## Configuration from environment

`PrestoPayClient.fromEnv()` builds a client from these environment variables:

| Variable | Required | Value |
|----------|----------|-------|
| `PRESTOPAY_ENV` | This or `PRESTOPAY_BASE_URL` | `staging` or `production` |
| `PRESTOPAY_BASE_URL` | This or `PRESTOPAY_ENV` | A gateway base URL, overriding `PRESTOPAY_ENV` |
| `PRESTOPAY_MID` | Yes | Your `mid` |
| `PRESTOPAY_KEYSTORE_PATH` | Yes | Path to your `.p12` keystore |
| `PRESTOPAY_KEYSTORE_PASSWORD` | Yes | The keystore's password |
| `PRESTOPAY_KEYSTORE_ALIAS` | No | The key's alias; needed only if the keystore holds more than one private key |
| `PRESTOPAY_PUBLIC_KEY_PATH` | Yes | Path to Presto's `.der` certificate |

A missing or empty variable throws `PrestoPayConfigException` naming it. `fromEnv()` uses the default timeouts
and retries; use the builder to change them.

## Loading keys from a secret store

The builder takes a `java.security.PrivateKey` and `PublicKey`, so keys don't have to be files. `PrestoPayKeys`
also reads them from streams, and takes the password as a `char[]` that you can clear after use:

```java
PrivateKey privateKey = PrestoPayKeys.privateKeyFromPkcs12(
    new ByteArrayInputStream(keystoreBytes), keystorePassword);          // add an alias if there are several keys
PublicKey prestoKey = PrestoPayKeys.publicKeyFromX509(new ByteArrayInputStream(certificateBytes));
Arrays.fill(keystorePassword, '\0');
```

## Several merchants

A client belongs to one `mid`: it sends that `mid` on every request and accepts webhooks only for it. One `mid`
can have several `prestoMrn`s, which you choose per request, so one client covers all of them.

To serve several merchants, build one client per `mid` (they can share the same keys) and route each request to
the matching client, for example with a `Map<String, PrestoPayClient>` keyed by `mid`. Give each merchant its
own `notifyUrl` too, so each webhook reaches the right client.

## Custom HTTP client

The client sends requests with the JDK's `HttpURLConnection`. To add a proxy, connection pooling or logging,
implement `HttpTransport` and pass it to `PrestoPayClient.builder().transport(...)`. To change the default
transport rather than replace it, wrap `HttpTransport.jdkDefault()`:

```java
HttpTransport jdk = HttpTransport.jdkDefault();
HttpTransport logging = (request, connectTimeout, readTimeout) -> {
    HttpResponse response = jdk.execute(request, connectTimeout, readTimeout);
    log.info("{} -> {}", request.url(), response.status());
    return response;
};
```

A transport must follow these rules, because the SDK's decision about when it's safe to retry `init`,
`reverse` and `refund` depends on them:

- Return every response as an `HttpResponse`, including non-2xx ones, rather than throwing.
- Don't follow redirects or resend requests.
- On an I/O failure, throw `PrestoPayTransportException` with `requestNotSent = true` only when the request
  certainly never left the process, such as a refused connection or a failed DNS lookup.

[`sample/custom-transport`](../sample/custom-transport) has complete transports built on Spring's `RestClient`,
the JDK 11+ `HttpClient` and OkHttp.

## Going live checklist

- [ ] Generate a separate key pair for production and register its public key with Presto.
- [ ] Use `Environment.PRODUCTION` with your production `mid`, `prestoMrn` and Presto certificate. Never mix
      staging and production values.
- [ ] Load the private key and its password from a secret store, not from source control or the image.
- [ ] Make `notifyUrl` a public HTTPS URL that Presto can reach.
- [ ] Have your return page `query` the payment instead of trusting the redirect.
- [ ] Have your webhook handler `query` the payment, deduplicate on `eventRefNum` under a unique constraint, and
      reply `NotifyAck.resend()` when your own processing fails.
- [ ] Handle an unknown outcome after a timeout or server error by querying, as in
      [Payments and errors](payments-and-errors.md#when-you-dont-know-whether-it-worked).
- [ ] Keep the server clock in sync with NTP.
- [ ] Log `errorCode()` and `errorMessage()` from `PrestoPayApiException`, so you can quote them to Presto
      support.

## Troubleshooting

**`1005` (`ErrorCode.EXCEEDED_VALIDITY_PERIOD`).** Your request's timestamp is too far from Presto's clock.
Sync the server clock with NTP. The SDK converts to the gateway's time zone itself, so the host's time zone
doesn't matter.

**`1006` or `1007` (`INVALID_SIGNATURE`, `SIGNATURE_VERIFICATION_FAILED`).** Presto couldn't verify your
signature. Usually the private key doesn't match the public key you registered for this environment, or you're
using a staging key in production or the other way round. To check what was signed, rebuild the canonical
string from a raw request body with `Canonicalizer.canonicalizeJson(json)`. Use that rather than the classes
under `com.prestouniverse.pay.internal`, which aren't part of the public API and can change in any release.

**`PrestoPaySignatureException` from a payment call.** Presto's response didn't verify with the certificate you
configured. Check that it's the certificate for this environment, and whether Presto has announced a new one.
`canonicalString()` holds the exact string that was verified.

**`1102` or `1106` (`INVALID_MID`, `INVALID_MERCHANT_REFERENCE`).** The `mid` or `prestoMrn` isn't valid for
this environment.

**Webhooks never arrive.** `notifyUrl` must be reachable from the internet. `localhost` and private addresses
won't work; during development, use a tunnel such as ngrok and pass its URL as `notifyUrl`.

**Webhooks are rejected with `PrestoPaySignatureException`.** Either the event is for a different `mid` than
the client's (check which `mid` the payment was created under), its timestamp is more than 15 minutes from your
clock (sync with NTP), or the Presto certificate is for the wrong environment.

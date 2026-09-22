# Plan: `presto-pay-sdk` — Java SDK for the Presto Payment Gateway

Status: draft v5 — gateway contract reconciled against
*PrestoConnect – Payment API Specification* (PDF; "spec" below), the
OpenAPI 3.0.1 definition embedded in `PrestoPay-Swagger.html` ("Swagger"),
and answers from Presto on 2026-09-22 (section 2.7). Where the documents
disagree, the Swagger wins for wire types and required fields; the PDF is
the only source for error codes, statuses, and process rules. No gateway
contract questions remain open. Implementation is under way in this repo
(`src/`, `pom.xml`) and is now genuinely zero-runtime-dependency: JSON is
hand-rolled (section 4.4), the only declared Maven dependency is
test-scoped JUnit.
Owner: TBD
Audience: any Java integrator of Presto Connect, internal or third-party. The
SDK is a standalone library with its own repo, build, and release cadence;
consumers depend on it only as a published Maven coordinate.

## 1. Goal

Build `presto-pay-sdk`: a framework-agnostic Java library that gives
integrators typed request/response models, correct request signing and
response/webhook verification, and key loading out of the box — instead of
each integrator hand-rolling HTTP calls, RSA canonicalization, and keystore
handling.

Ergonomics follow the OpenAI Java SDK and Stripe Java: a builder-constructed,
thread-safe, reusable client; typed `*Params` builders; typed responses;
domain-scoped sub-clients (`client.payments()`, `client.webhooks()`).

## 2. Gateway contract

Everything here is taken from the spec, the Swagger, or Presto's answers
(section 2.7). Spec section numbers are given as *(spec section x)*.

### 2.1 Endpoints and environments *(spec sections 4.1, 4.2, 5, 6)*

| Operation | Endpoint | Notes |
|---|---|---|
| Init | `POST /v1/ext/payment/init` | returns `paymentUrl` (empty if already finalised) |
| Query | `POST /v1/ext/payment/query` | by `paymentRefNum` **or** `txnRefNum` (`paymentRefNum` wins if both) |
| Reverse | `POST /v1/ext/payment/reverse` | within 15 min of init/authorise; cancels if unauthorised, reverses if authorised |
| Refund | `POST /v1/ext/payment/refund` | full (no `amount`) or partial (`amount`); partial is per-partner enabled |
| Webhook | Presto `POST`s to `notifyUrl` | must answer HTTP 200 with `{"resend": false}` |

`/ext/user/verify` (userToken → `payerRefNum`, mini-app flow) is mentioned
but not specified in this document; out of scope for v1.

Environments (separate credentials each):

| `Environment` | Base URL |
|---|---|
| `STAGING` | `https://presto-stg-ext.enovax.com` |
| `PRODUCTION` | `https://pay-ext.prestouniverse.com` |

Always use the explicit `/v1` prefix; unversioned URIs auto-route to latest.

### 2.2 Merchant identity *(spec sections 2, 4.3)*

- `mid` — **Master Merchant** reference (the integrating partner). The RSA
  key pair is exchanged per partner, i.e. per `mid` per environment.
- `prestoMrn` — sub-merchant reference under that master merchant. A partner
  serving many merchants sends a different `prestoMrn` per request.

Both are request-level fields in the SDK: the client builder holds optional
defaults, each `*Params` builder can override. Because the signing key is
tied to `mid`, overriding `mid` per request is only meaningful if the same
key pair is registered for both mids; `prestoMrn` is the normal per-request
axis.

### 2.3 Signing and verification *(spec sections 4.1, 4.3.1)*

1. Collect all fields in the request body, excluding `signature`. Absent
   optional fields are simply not present (the spec example body has 10
   fields and its canonical string has 10 values).
2. Sort ascending by key. All spec keys are lowerCamel ASCII, so
   `String.compareTo` order matches the spec example.
3. Concatenate **values only** with `:`. `null` → `""` but the separator is kept.
4. `SHA256withRSA` with the partner's RSA private key; standard Base64,
   no line breaks → `signature`.
5. Add `signature` to the body.
6. Responses and webhooks: same steps, verify with Presto's public key.
7. Every request carries `ts` — `yyyyMMddHHmmss.SSS` in **UTC+8**. Presto
   rejects requests older than **15 minutes**.

Canonical rendering per value type:

| Value type | Rendering | Source |
|---|---|---|
| string | as-is (spaces, `#`, URLs untouched) | spec example: `Order #12345` |
| integer | JSON literal text | spec example: `1200` |
| boolean | `true` / `false` | JSON boolean on the wire; confirmed by Presto (section 2.7) |
| null | `` (empty), separator kept | spec section 4.3.1 |
| absent key | not present | spec example |
| JSON-encoded string | as-is, i.e. the raw JSON text | Swagger: `itemList`, `transactionalData`, `modeData`, `bindData`, `paymentDetails`, `refundDetails` are `type: string`; webhook example `paymentDetails` = `[{"method":"Wallet","amount":5000}]` |
| empty string | `` (same as null) | Swagger webhook example `additionalData: ""` |
| native array | compact JSON text, e.g. `["Wallet","Card"]` (no spaces) | `allowedPaymentMethods` is the only non-scalar on the wire (Swagger `type: array, items: string`); rendering confirmed by Presto (section 2.7) |
| nested object | never occurs | no `type: object` property anywhere in the Swagger |

The worked example (body → sorted keys → canonical string) appears in both
the PDF and the Swagger `info.description`, with slightly different
`redirectUrl` values; both are canonicalization test vectors (section 7). Response
examples omit absent optional fields rather than sending `null`.

Amounts are integer **cents** everywhere (`amount`, `refundAmount`,
`unitAmount`, `totalAmount`); `int` in the Java API. `currencyCode` is ISO
4217; only `MYR` is accepted today.

### 2.4 Errors *(spec sections 4.4, 7.2)*

| Kind | HTTP | Where the error is | Signed? |
|---|---|---|---|
| System error (bad signature, stale `ts`, malformed body, wrong input, unexpected failure) | **400** | response headers `x-http-error-code`, `x-http-error` (read case-insensitively) | **empty body**; nothing to verify |
| Business error (insufficient balance, duplicate `txnRefNum`, …) | **200** | body `{"ts", "success": false, "errorCode", "errorMessage", "signature"}` | yes |

Every successful body also carries `success: true`. Error codes are 4-digit
strings, namespaced: `10xx` request/auth, `11xx` merchant, `12xx` payment,
`14xx` user (full table in spec section 7.2). Notable for the SDK:

- `1203` duplicate `txnRefNum` for the `prestoMrn` — an `init` retry with
  the same `txnRefNum` cannot double-charge.
- `1224` / `1226` reversal/refund already in progress — cannot be re-initiated.
- `1005` exceeded validity period — client clock skew.

### 2.5 Key material *(spec sections 4.1, 7.3)*

- Partner private key: PKCS12 keystore generated with `keytool` (RSA 2048), password + alias.
- Presto public key: X.509 certificate, **DER** (`.der`). The SDK also accepts PEM.

### 2.6 Webhook *(spec section 6)*

Presto `POST`s to the `notifyUrl` given on init/reverse/refund. Body: `mid`,
`prestoMrn`, `eventCode`, `paymentRefNum`, `txnRefNum`, `userRefNum`,
`success`, `eventRefNum`, `eventTs`, `amount`, `currencyCode`,
`additionalData`, `paymentDetails` (JSON string), `ts`, `signature`.

`eventCode` ∈ {`Authorised`, `Cancelled`, `Reversed`, `Refunded`, `Expired`};
`success` says whether the underlying event succeeded (e.g. `Authorised` +
`success=false` means payment status `Failed`). The spec recommends calling
`query` on receipt for the authoritative status.

Acknowledgement: HTTP **200** with body `{"resend": false}`. Unacknowledged
notifications are resent at 2, 4, 8, … 1024 minutes, then dropped. The
integrator should ack before doing any work.

### 2.7 Confirmed with Presto (2026-09-22)

| Question | Answer |
|---|---|
| `allowedPaymentMethods` canonical rendering | JSON array text `["Wallet","Card"]` |
| `success` on the wire / in the canonical string | JSON boolean; `true` / `false` |
| Base64 | standard alphabet, no line breaks |
| HTTP 400 | headers `x-http-error-code: 1006`, `x-http-error: Invalid request.`; **body empty** |
| HTTP 200 business error | `{"success": false, "ts": "…", "errorCode": "1201", "errorMessage": "Invalid input.", "signature": "…"}` — `errorCode` is a string; canonical string is `1201:Invalid input.:false:<ts>` |
| `reversalStatus` values | `Reversing`, `Failed`, `Success` |

A real signed business-error body was supplied with these answers; it is
checked in as a canonicalization vector and, once the staging DER
certificate is available to the test suite, as a verification vector.

Known document drift, handled in the SDK rather than open: the Swagger's
query response schema lists fewer fields than the PDF (no `reversalRefNum`,
`refundStatus`, `refundRefNum`, …). The SDK parses every PDF field as
optional and ignores unknown fields.

## 3. Design principles

1. **Java 8 bytecode baseline.** Payment integrators skew conservative and
   third-party consumers can't be assumed to be on 11+. Cost: no
   `java.net.http.HttpClient`, no `var`/records/`List.of`, no
   `module-info.java` (ship `Automatic-Module-Name` instead). Revisit only
   if every known consumer is confirmed on 11+.
2. **Zero runtime dependencies.** JSON is a hand-rolled minimal
   parser/writer (section 4.4.1); HTTP is the JDK's `HttpURLConnection` by
   default (section 4.5). A consumer's classpath gains nothing by adding
   this SDK — no transitive version to conflict with, ever.
3. **No framework coupling.** Plain Java; Spring/Quarkus/CLI users write
   their own few lines of wiring. No starter module.
4. **Thread-safe, reusable client.** Build once, share.
5. **Typed, immutable params and responses.** Hand-written builders that
   validate required fields and spec constraints (max lengths, mutually
   exclusive fields) in `build()`. No `Map<String, Object>` in the public API.
6. **Gateway-defined codes are open enums.** A new code from the gateway
   must never make parsing throw.
7. **Unchecked, SDK-owned exception hierarchy** (section 4.7).
8. **Signing is testable in isolation.** The canonical string is a public,
   pure function with pinned test vectors, separate from the signature.
9. **Webhook verification does not require a client.** A receiver that
   never calls the gateway needs only Presto's public key.
10. **No logging dependency.** Deferred to a later release; when added,
    it will be `java.util.logging` (JDK-built-in) to keep the
    zero-dependency guarantee, with the redaction rules in section 4.9
    applying regardless of backend.
11. **Explicit timeouts and per-operation retry policy** (section 4.8).
12. **Style is enforced by the build, not by review.** Checkstyle runs on
    every build and fails it on violation (section 4.12).

## 4. Architecture

### 4.1 Module layout

Single Maven module, single repo. Public API is everything outside
`internal`; `internal` may change without notice.

```
presto-pay-sdk/
└── src/main/java/com/prestouniverse/pay/
    ├── PrestoPayClient.java          builder + facade
    ├── Environment.java              STAGING / PRODUCTION (host baked in) + custom baseUrl
    ├── RetryPolicy.java              per-operation retry configuration
    ├── PrestoPayKeys.java            PKCS12 / X.509 (DER or PEM) loaders (InputStream + Path overloads)
    ├── payments/
    │   ├── PaymentsClient.java       init / query / reverse / refund
    │   ├── PaymentInitParams.java    PaymentInitResponse.java
    │   ├── PaymentQueryParams.java   PaymentQueryResponse.java   (by paymentRefNum or txnRefNum)
    │   ├── PaymentReverseParams.java PaymentReverseResponse.java
    │   ├── PaymentRefundParams.java  PaymentRefundResponse.java
    │   ├── TxnType.java              open enum: QrPay / WebPay / MiniAppPay
    │   ├── PaymentStatus.java        open enum: PendingAuthorise / Cancelled / Authorised / Failed / PendingReverse /
    │   │                                        Reversed / PendingRefund / PartialRefunded / Refunded / Expired
    │   ├── RefundStatus.java         open enum: Refunding / Failed / Success
    │   ├── ReversalStatus.java       open enum: Reversing / Failed / Success
    │   ├── PaymentMethod.java        open enum: Wallet / CashBack / Card / GrabPay / TouchNGo / Maybank / ... (spec section 5.2)
    │   ├── PaymentDetail.java        parsed element of the paymentDetails JSON string (method, amount, cardBin, cardSummary, cardType)
    │   ├── RefundDetail.java         parsed element of the refundDetails JSON string
    │   ├── LineItem.java             element of itemList (unitAmount / totalAmount in cents)
    │   └── ErrorCode.java            constants for spec section 7.2 (DUPLICATE_TXN_REF = "1203", ...)
    ├── webhooks/
    │   ├── WebhookVerifier.java      standalone; built from PublicKey only
    │   ├── NotifyEvent.java          typed, immutable webhook payload
    │   ├── NotifyEventCode.java      open enum: Authorised / Cancelled / Reversed / Refunded / Expired
    │   └── NotifyAck.java            builds the {"resend": false} response body
    ├── crypto/
    │   ├── Canonicalizer.java        JSON object tree -> canonical String (section 2.3)  [public: debugging aid]
    │   └── RsaSignatureService.java  sign(canonical) / verify(canonical, sig)
    ├── internal/json/                hand-rolled JSON, no external dependency (section 4.4.1)
    │   ├── JsonObject.java           ordered string-keyed map; put/get/putArray/putObject/deepCopy
    │   ├── JsonArray.java            ordered value list
    │   ├── JsonParser.java           package-private recursive-descent parser
    │   └── JsonWriter.java           package-private compact serializer
    ├── http/
    │   ├── HttpTransport.java        interface: HttpResponse execute(HttpRequest)
    │   ├── HttpRequest.java          method, url, headers, body (String)
    │   └── HttpResponse.java         status, headers, body (String)
    ├── exception/
    │   ├── PrestoPayException.java            base (unchecked)
    │   ├── PrestoPayApiException.java         gateway rejected: httpStatus, errorCode, errorMessage, isSystemError()
    │   ├── PrestoPaySignatureException.java   canonicalization / verify failure
    │   ├── PrestoPayTransportException.java   I/O, timeout, TLS; requestNotSent()
    │   └── PrestoPayConfigException.java      bad keys, missing required builder fields, constraint violations
    └── internal/
        ├── JdkHttpTransport.java     default HttpURLConnection transport
        ├── JsonCodec.java            typed accessors over internal/json; models stay annotation-free
        ├── RequestPipeline.java      params -> JSON tree -> canonicalize -> sign -> send -> verify -> parse
        └── Timestamps.java           ts in yyyyMMddHHmmss.SSS at fixed offset +08:00
```

Maven coordinates: `com.prestouniverse:presto-pay-sdk`, published to Maven
Central. Base package `com.prestouniverse.pay`. Source on GitHub.

### 4.2 Public API sketch

```java
// Explicit construction
PrestoPayClient client = PrestoPayClient.builder()
    .environment(Environment.STAGING)                 // or .baseUrl("https://...") to override
    .merchantId(mid)                                  // optional default; tied to the key pair
    .merchantRefNum(defaultPrestoMrn)                 // optional default; any request can override
    .privateKey(PrestoPayKeys.privateKeyFromPkcs12(keystoreStream, password /* char[] */, alias))
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(derStream))
    .connectTimeout(Duration.ofSeconds(5))            // defaults shown
    .readTimeout(Duration.ofSeconds(30))
    .retryPolicy(RetryPolicy.defaults())
    .transport(myTransport)                           // optional; share one across clients
    .build();

// Env-var driven
PrestoPayClient client = PrestoPayClient.fromEnv();
// PRESTOPAY_ENV=staging|production   (or PRESTOPAY_BASE_URL)
// PRESTOPAY_MID, PRESTOPAY_MRN                     (optional defaults)
// PRESTOPAY_KEYSTORE_PATH, PRESTOPAY_KEYSTORE_PASSWORD, PRESTOPAY_KEYSTORE_ALIAS
// PRESTOPAY_PUBLIC_KEY_PATH

PaymentInitResponse init = client.payments().init(PaymentInitParams.builder()
    .merchantRefNum(prestoMrn)                        // overrides the client default for this call
    .txnType(TxnType.WEB_PAY)
    .txnRefNum(orderRefNum)                           // <= 50 chars
    .displayDesc("Payment for " + orderRefNum)        // <= 255 chars
    .amount(10000)                                    // cents: RM 100.00; omit if unknown
    .currencyCode("MYR")                              // required when amount is set
    .notifyUrl(notifyUrl)
    .redirectUrl(redirectUrl)                         // required when txnType == WebPay
    .sessionValidity(Instant.now().plus(Duration.ofMinutes(10)))   // rendered as ts format, UTC+8
    .allowedPaymentMethods(PaymentMethod.WALLET, PaymentMethod.CARD)
    .items(LineItem.builder().itemDesc("Widget").quantity(1).unitAmount(10000).totalAmount(10000).build())
    .build());                                        // PrestoPayConfigException: missing field, qrValue+payerRefNum together, length overflow
String redirectTo = init.paymentUrl();

// Query by paymentRefNum, or by txnRefNum when init never returned
PaymentQueryResponse status = client.payments().query(
    PaymentQueryParams.builder().paymentRefNum(paymentRefNum).build());
PaymentQueryResponse status = client.payments().query(
    PaymentQueryParams.builder().txnRefNum(orderRefNum).build());
if (status.paymentStatus().equals(PaymentStatus.AUTHORISED)) { ... }
status.paymentStatus().isKnown();                     // false for a code this SDK version doesn't list
List<PaymentDetail> methods = status.paymentDetails(); // parsed from the JSON string, lazily
List<RefundDetail> refunds = status.refundDetails();

PaymentReverseResponse rev = client.payments().reverse(PaymentReverseParams.builder()
    .paymentRefNum(paymentRefNum)                     // or .txnRefNum(...)
    .reversalRefNum(reversalId)                       // <= 50 chars; may reuse txnRefNum for cancellation
    .remark(reason)
    .build());

PaymentRefundResponse refund = client.payments().refund(PaymentRefundParams.builder()
    .paymentRefNum(paymentRefNum)
    .refundRefNum(refundId)                           // <= 50 chars
    .remark(reason)                                   // required, <= 200 chars
    .amount(refundAmountCents)                        // omit for a full refund
    .build());
refund.refundAmount();                                // final refunded cents

// Webhooks: standalone verifier, no client needed
WebhookVerifier verifier = WebhookVerifier.builder()
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(derStream))
    .maxTimestampAge(Duration.ofMinutes(15))          // optional replay guard on ts; off by default
    .build();
NotifyEvent event = verifier.parse(rawBody);          // throws PrestoPaySignatureException
event.eventCode(); event.success(); event.paymentRefNum(); event.txnRefNum(); event.eventTs();
String ackBody = NotifyAck.ok();                      // {"resend":false} — respond HTTP 200 with this

// Same thing via a client, for integrators that already have one
NotifyEvent event = client.webhooks().parse(rawBody);
```

### 4.3 Request/response pipeline

```
params.build()
  -> resolve mid / prestoMrn: params value, else client default, else PrestoPayConfigException
  -> JSON object tree: only fields that were set (absent, not null), plus ts (now, UTC+8)
  -> Canonicalizer.canonicalize(tree)            pure, public, test-vector target
  -> RsaSignatureService.sign(canonical)         -> add "signature" to tree
  -> JsonCodec.serialize(tree)                   -> HttpRequest
  -> HttpTransport.execute()                     -> HttpResponse
  -> if HTTP != 200:
       read x-http-error-code / x-http-error headers -> PrestoPayApiException(isSystemError = true)
       (body is empty for system errors; nothing to verify)
  -> JsonCodec.parseTree(body)                   numbers kept as literal text; never double
  -> RsaSignatureService.verify(canonicalize(tree - signature))   -> PrestoPaySignatureException
  -> if success == false: PrestoPayApiException(errorCode, errorMessage, isSystemError = false)
  -> map to typed response
```

The tree that is canonicalized is the same tree that is serialized, so
request-side drift between "what was signed" and "what was sent" is
impossible by construction. Response-side, canonicalization reads the parsed
tree with numbers kept as their literal text.

Business errors are signed, so verification runs before `success` is
inspected: a tampered error response is a signature failure, not an API
error.

### 4.4 JSON

No JSON library dependency. The request/response shape this SDK needs is
narrow and fully known (section 2): flat objects, integer numbers only (no
floats/`BigDecimal`), one level of string arrays, and JSON-encoded strings
for the structured fields. That's a small, stable surface, so
`internal/json` implements it directly: `JsonObject` (ordered string-keyed
map), `JsonArray` (ordered list), a package-private recursive-descent
`JsonParser`, and a package-private `JsonWriter`. Values held in an object
or array are `String`, `Integer`/`Long`, `Boolean`, `JsonObject`,
`JsonArray`, or `null` — no custom node-type hierarchy to maintain.

`internal/JsonCodec` sits on top as the typed-accessor layer
(`requiredText`, `optInt`, `optBoolean`, `putIfPresent`, ...) that the rest
of the SDK actually calls, so `Canonicalizer` and every `*Params`/`*Response`
class depend on `JsonCodec` + `JsonObject`/`JsonArray`, never on the parser
or writer directly.

- Public models carry no JSON-library types in their signatures beyond
  `JsonObject`/`JsonArray` themselves, which are part of this SDK, not a
  dependency.
- String-typed JSON fields (`itemList`, `paymentDetails`, `refundDetails`,
  `modeData`, `bindData`) are serialized to a compact JSON string by the
  params builder and parsed lazily by the response accessors, so they
  participate in the canonical string as ordinary strings.
- `allowedPaymentMethods` is a native JSON array on the wire. `Canonicalizer`
  renders arrays as compact JSON text (`["Wallet","Card"]`, no whitespace),
  matching what the writer serializes.
- Response parsing is lenient: unknown fields are ignored (the Swagger and
  PDF field lists differ), missing optionals are `null`, never an exception.
- The parser accepts the full JSON grammar (objects, arrays, strings with
  standard/`\uXXXX` escapes, numbers, booleans, `null`) so it degrades
  gracefully — as a parse error, not a crash — on any well-formed JSON the
  gateway might send that this SDK doesn't otherwise expect.

#### 4.4.1 Why hand-rolled instead of Jackson/Gson

A general-purpose JSON library is the one dependency most likely to
version-conflict in a consumer's application (design principle 2). Given
the actual surface is this small, and every field this SDK reads or writes
is already enumerated in section 2, a ~250-line internal parser/writer is
less risk than importing and pinning a general-purpose library, and it
removes the "Jackson matrix" testing burden (former section 8 #5 decision)
entirely. The trade-off is explicit: this SDK owns JSON correctness
(escaping, Unicode, malformed input) itself rather than inheriting it from
a hardened third-party library — mitigated by the parser accepting the full
grammar (not just the fields we expect) and by the canonicalization/
round-trip test vectors in section 7 exercising it directly.

### 4.5 Transport

Default is `internal/JdkHttpTransport` on `HttpURLConnection`: JDK-only,
works on Java 8, sufficient for four low-volume POSTs, and adds no
transitive dependencies (OkHttp would pull in Kotlin stdlib + okio for every
consumer). Stripe Java ships the same default.

`HttpTransport` is a public, minimal, JSON-agnostic interface so an
integrator can plug in OkHttp, Apache HttpClient, or a proxy-aware client. An
optional `presto-pay-sdk-okhttp` artifact can be added later if there is
demand; it is not part of v1.

Transport requirements: connect/read timeouts honoured per request; TLS uses
JDK defaults with no trust-all escape hatch; `Content-Type: application/json;
charset=UTF-8` (spec error `1002` is "invalid content type");
`User-Agent: presto-pay-sdk/<version> java/<version>`; response headers are
exposed with case-insensitive lookup so the pipeline can read
`x-http-error-code` / `x-http-error`.

### 4.6 Key material

`PrestoPayKeys` static loaders take `InputStream` (primary) with `Path`
convenience overloads. Passwords are `char[]`. `publicKeyFromX509` accepts
DER (what Presto issues) or PEM. The builder takes `PrivateKey`/`PublicKey`
directly, so integrators sourcing keys from Vault, KMS, or an HSM never
touch file I/O. Bridging a framework's resource abstraction (`classpath:`)
into an `InputStream` is the consumer's one-liner.

### 4.7 Errors

All unchecked, rooted at `PrestoPayException`:

| Exception | When | Carries |
|---|---|---|
| `PrestoPayApiException` | HTTP 400 system error, or HTTP 200 with `success: false` | `httpStatus`, `errorCode`, `errorMessage`, `isSystemError()`, raw body; `errorCode` comparable to `ErrorCode` constants |
| `PrestoPaySignatureException` | response/webhook signature invalid, or payload can't be canonicalized | which side (response/webhook), the canonical string (DEBUG only) |
| `PrestoPayTransportException` | I/O, timeout, TLS, DNS | cause, `requestNotSent()` |
| `PrestoPayConfigException` | builder validation (required fields, max lengths, `qrValue`/`payerRefNum` exclusivity, `amount` without `currencyCode`), bad keys, missing env vars | field name |

### 4.8 Timeouts and retry

Defaults: connect 5s, read 30s.

`RetryPolicy` is per operation because retries are not uniformly safe:

| Operation | Default retry | Rationale (spec) |
|---|---|---|
| `query` | up to 2 retries on transport error or 5xx, exponential backoff | read-only |
| `init` | retry only when `requestNotSent()` | a retry after a read timeout with the same `txnRefNum` is rejected with `1203` (no double charge) — but the right recovery is `query(txnRefNum)`, which the spec recommends for exactly this case |
| `reverse`, `refund` | retry only when `requestNotSent()` | `1224`/`1226`: an in-progress reversal/refund cannot be re-initiated; reconcile via `query` |

Never retry on `PrestoPayApiException` or `PrestoPaySignatureException`.
README documents the "init timed out → `query` by `txnRefNum`" pattern.

### 4.9 Logging

Not in v1 — the SDK has no logging dependency and emits nothing on its own
(section 3 principle 10). When added, it will be `java.util.logging`
(JDK-built-in, keeps the zero-dependency guarantee) with these rules
regardless of when it lands:

- Never log the keystore password, private key, or keystore bytes at any level.
- Full request/response bodies and canonical strings only at the most
  verbose level.
- Any higher-level line carries `txnRefNum`/`paymentRefNum` and `errorCode`
  only.

### 4.10 Open enums

`TxnType`, `PaymentStatus`, `RefundStatus`, `ReversalStatus`, `PaymentMethod`,
`NotifyEventCode` follow one pattern: a final class with static constants
for the values in spec sections 5-7, `of(String)` that returns a known constant or
wraps the raw value, `value()`, `isKnown()`, and equality on the raw value.
Parsing never throws on an unknown code. `ErrorCode` is a constants holder
only (`errorCode` stays a `String` on the exception).

### 4.11 Time

`ts`, `sessionValidity`, `eventTs`, and all `*Date` fields use
`yyyyMMddHHmmss.SSS` at UTC+8. `internal/Timestamps` formats and parses with
a fixed `ZoneOffset.of("+08:00")`, never the JVM default zone. The public
API exposes `Instant` (or `OffsetDateTime`) and does the conversion; `ts`
generation uses an injectable `Clock` for tests.

### 4.12 Build and static analysis

`maven-checkstyle-plugin` runs in the `validate` phase and again in
`verify` via `check` (not just `checkstyle:checkstyle` reporting), so a
violation fails `mvn install`/`mvn verify` locally and in CI — style is a
build gate, not a post-hoc report nobody reads.

- **Ruleset**: `config/checkstyle/checkstyle.xml`, checked into the repo,
  based on `google_checks.xml` with two deltas: line length 120 (not 100,
  to match the builder-chain style used throughout section 4.2), and
  `JavadocMethod`/`JavadocType` scoped to `public`/`protected` members only
  — `internal/**` is exempt via `config/checkstyle/suppressions.xml`
  (`SuppressionFilter`), since section 4.1 already treats that package as
  free to change without notice. Google's ruleset is picked over Sun's
  defaults because it also enforces import order and forbids wildcard
  imports, which matters once `crypto`/`http`/`exception` packages import
  from each other.
- **Severity**: `violationSeverity=error`; `failOnViolation=true`;
  `failsOnError=true`. No warning-only mode — a library with one committer
  today and unknown reviewers later needs the build itself to hold the
  line, not convention.
- **Scope**: `src/main/java` and `src/test/java` both checked; test code
  gets the same import-order and brace rules but not the Javadoc rules
  (`TestClass`/`TestMethod` excluded via the suppressions file).
- **License/header check**: out of scope for v1 — revisit once section 8
  #1's repo is public and a LICENSE file exists to derive a header from.
- **IDE integration**: not the SDK's concern; `config/checkstyle/checkstyle.xml`
  is a standard path IntelliJ's CheckStyle-IDEA plugin and VS Code's
  Checkstyle extension both auto-discover, so contributors get live
  feedback without extra setup docs.

## 5. Non-goals

- No persistence, no ORM, no payment-state store.
- No business logic (order state machines, fee calculation, inventory).
- No scheduler integration; `query()` must simply be cheap and safe to call repeatedly.
- No UI or redirect-landing page.
- No webhook HTTP endpoint; the SDK verifies/parses a body it is handed and builds the ack body. Returning HTTP 200 is the integrator's route.
- No `/ext/user/verify` in v1 (not specified in this document).
- No Spring Boot starter.
- No async API in v1 (section 8 #6).

## 6. Delivery plan

1. **Pin test vectors.** Start from the PDF and Swagger worked examples
   (body → canonical string), the Swagger request/response/webhook
   examples, and the real business-error body from section 2.7. Add
   vectors for: a null `amount`, an omitted optional field, an empty-string
   field, a JSON-string field, an `allowedPaymentMethods` array, a webhook
   body with `success: true` and `false`, Unicode in `displayDesc`, and one
   per endpoint. Sign them with a throwaway test RSA key pair. Confirm the
   whole set with one real `init` + `query` against staging before step 2
   ends.
2. **Build.** Scaffold the `pom.xml` with the Checkstyle gate (section
   4.12) enabled from the first commit, so no code is ever written against
   a lower bar than what ships. Then `Canonicalizer` + `RsaSignatureService`
   first, against the vectors. Then transport, codec, pipeline,
   params/responses (init, query, reverse, refund), webhooks, client
   builder, `fromEnv()`. Unit + contract tests per section 7.
3. **Publish** `1.0.0` to Maven Central with a README (including the
   init-timeout → `query(txnRefNum)` recovery pattern and the webhook ack
   contract), CHANGELOG, and a Spring wiring example in `docs/`. Central
   prerequisites, set up once in the GitHub repo's CI: `com.prestouniverse`
   namespace verified on the Sonatype Central Portal, GPG-signed artifacts,
   sources and javadoc jars, POM with license/SCM/developer metadata,
   release via the `central-publishing-maven-plugin` on a tagged build.

Done when a versioned artifact is installable by anyone with the
coordinates. Adoption by any specific consumer is separate work in that
consumer's repo.

## 7. Testing

- **Canonicalization vectors**: the PDF section 4.3.1 and Swagger `info.description` examples, the Swagger webhook example (`additionalData: ""`, JSON-string `paymentDetails`, boolean `success`), the real business-error body from section 2.7, plus those from section 6 step 1; JSON → canonical string, asserted independently of signing.
- **Lenient parsing**: response bodies with extra unknown fields and with every optional absent parse without error.
- **Signature vectors**: canonical string → signature and verify, with the checked-in test key pair (never a real partner key).
- **Round-trip consistency**: for every params type, the canonical string the pipeline signs equals the canonical string recomputed from the body bytes actually handed to `HttpTransport`.
- **Timestamp tests**: `ts` is rendered at +08:00 regardless of JVM default zone; `sessionValidity` conversion; parse of `paymentRequestDate` etc.
- **Contract tests** against a mock server (JDK `com.sun.net.httpserver.HttpServer`, no extra dependency) that **signs its responses** with the test key pair: request shape and headers for all four endpoints, response mapping, `signature`-stripped verification, HTTP 400 with `x-http-error-*` headers and empty body, HTTP 200 with `success: false`.
- **Negative tests**: tampered response signature → `PrestoPaySignatureException`; 400 → `PrestoPayApiException(isSystemError)`; 200 + `success:false` → `PrestoPayApiException` with `errorCode`/`errorMessage`; malformed webhook body → `PrestoPaySignatureException`, never an NPE; unknown status/method/event code → parses, `isKnown() == false`; builder constraint violations (missing required, `qrValue` + `payerRefNum`, `amount` without `currencyCode`, WebPay without `redirectUrl`, length overflow) → `PrestoPayConfigException`; request with no `mid`/`prestoMrn` on params or client → `PrestoPayConfigException`.
- **Retry tests**: `init`/`reverse`/`refund` not retried after a read timeout; `query` is; connect refused triggers retry for all.
- **Staging smoke test**: opt-in via env vars, excluded from default `mvn test`; real init + query against `Environment.STAGING`.
- **Style gate**: `mvn verify` (both locally and in CI) fails on any Checkstyle violation (section 4.12); no separate "lint" job to remember to run.

## 8. Decisions

| # | Question | Status / default |
|---|---|---|
| 1 | Repo host/org | **Resolved**: GitHub, standalone repo |
| 2 | groupId / package | **Resolved**: `com.prestouniverse` / `com.prestouniverse.pay` |
| 3 | Publish target | **Resolved**: Maven Central |
| 4 | Transport default | **Resolved**: JDK `HttpURLConnection`; OkHttp as optional later artifact |
| 5 | JSON library | **Resolved**: none — hand-rolled `internal/json` (section 4.4.1), zero runtime dependencies |
| 6 | Async API | Deferred to v2; `HttpTransport` stays sync-only in v1 |
| 7 | Business errors: 2xx + code, or non-2xx? | **Resolved by spec section 4.4 + Presto sample**: business errors are HTTP 200 + `success:false`; system errors are HTTP 400 + `x-http-error-*` headers, empty body. Both → `PrestoPayApiException` |
| 8 | Are error responses signed? | **Resolved**: 200 business errors yes (verify first); 400 system errors have no body |
| 9 | `init`/`refund` idempotency | **Resolved by spec**: not idempotent, but duplicates are rejected (`1203`, `1224`, `1226`) rather than re-executed. No retry after send; recover via `query(txnRefNum)` |
| 10 | Webhook ack | **Resolved by spec section 6**: HTTP 200 + `{"resend": false}`; resend backoff 2…1024 min |
| 11 | Nested objects/arrays | **Resolved by Swagger + Presto**: no objects; all structured fields are JSON strings except `allowedPaymentMethods`, a native string array canonicalized as `["Wallet","Card"]` |
| 12 | Amount format, sort collation, Base64, boolean rendering | **Resolved**: integer cents; `String.compareTo` sort; standard Base64 without line breaks; `success` → `true`/`false` |
| 13 | Java 8 vs 11 baseline | **Resolved**: 8, per section 3 #1 |
| 14 | Compatibility policy | Semver; public API = everything outside `internal`; `1.x` never removes a public symbol |
| 15 | Is the key pair per merchant or per environment? | **Resolved by spec section 4.3**: per partner (`mid`) per environment. One client = one `mid` + key pair; `prestoMrn` varies per request. Per-request `mid` override stays available but is only valid if the key is registered for that `mid` |
| 16 | Timestamp zone | **Resolved by spec section 4.1**: UTC+8 fixed offset, 15-minute validity |
| 17 | `/ext/user/verify` | **Resolved**: out of scope |
| 18 | `reversalStatus` values | **Resolved by Presto**: `Reversing` / `Failed` / `Success` (`ReversalStatus` open enum) |
| 19 | Style enforcement | **Resolved**: Checkstyle, Google-based ruleset, build-breaking (section 4.12) |

## 9. Risks

- **Canonicalization drift** is the only risk that breaks every call at
  once. Mitigation: the spec example as vector zero, further vectors from
  captured traffic, the round-trip consistency test, and canonicalizing over
  the wire JSON tree rather than Java objects.
- **Clock skew**: Presto rejects `ts` older than 15 minutes (`1005`). The SDK
  uses UTC+8 explicitly, but an integrator host with a badly drifted clock
  will see every request fail. Surface `1005` clearly in the exception
  message.
- **Swagger vs PDF drift**: the two documents already disagree on field
  types and field lists. Treat the Swagger as the wire truth, parse
  leniently, and re-check both when Presto publishes a new version.
- **Hand-rolled JSON correctness** is now this SDK's responsibility instead
  of a hardened library's. Mitigation: the parser implements the full JSON
  grammar rather than a subset tailored to today's fields, and section 7's
  canonicalization/round-trip vectors exercise it on every build; revisit
  only if a real-world payload class turns out to need something the
  parser doesn't handle.
- **Maven Central onboarding** (namespace verification, GPG keys, CI
  secrets) is a one-time setup with a lead time of days, not hours; start
  it in parallel with step 2 so it doesn't gate the first release.

# Agent guide — presto-pay-sdk

Instructions for AI coding agents working in this repository. Human integrators should use [README.md](README.md).

## What this repo is

Standalone **Java 8+** Maven library (`com.prestouniverse:presto-pay-sdk`) for the **Presto Connect** payment gateway: typed `*Request` / response models, RSA request signing, response and webhook verification, PKCS#12 / X.509 key loading. **No runtime dependencies** (JUnit 5 is test-only). **Thread-safe** `PrestoPayClient` — build once, share across threads.

Gateway behavior and wire formats are defined by the implemented client, [README.md](README.md), and contract tests (especially `PrestoPayClientContractTest`). When changing API shapes or signing, read those and match existing request/response types.

## Commands

| Goal | Command |
|------|---------|
| Default CI gate (Checkstyle + unit tests) | `mvn --batch-mode verify` |
| Live staging smoke (credentials required) | `PRESTOPAY_STAGING_SMOKE=1` + env vars, then `mvn verify -Pstaging-smoke` |
| Attach sources/Javadoc jars | `mvn verify -Prelease` |

CI (`.github/workflows/ci.yml`) runs `mvn verify` on JDK 8.

## Layout

```
src/main/java/com/prestouniverse/pay/
  PrestoPayClient.java      # Builder, fromEnv(), entry point
  PrestoPayKeys.java        # PKCS#12 / X.509 loading
  Environment.java          # STAGING / PRODUCTION base URLs
  RetryPolicy.java          # Transport/API retry backoff
  SdkVersion.java           # From filtered presto-pay-sdk-version.properties
  payments/                 # Public payment API (*Request, responses, constants)
  webhooks/                 # WebhookVerifier, NotifyEvent, NotifyAck
  crypto/                   # Canonicalizer (public), RsaSignatureService
  http/                     # HttpTransport SPI (public)
  exception/                # PrestoPay*Exception hierarchy
  internal/                 # NOT semver-stable — JSON codec, RequestPipeline, JDK transport
src/test/java/              # Contract tests, crypto tests, MockGatewayServer support
config/checkstyle/          # Checkstyle rules (fail on violation)
docs/spring-wiring.md       # Optional Spring @Configuration example
```

**Module name:** `com.prestouniverse.pay` (JPMS automatic module).

## Public API boundary

| Safe for integrators | Agent may change with semver care |
|----------------------|-----------------------------------|
| Everything except `internal` | `com.prestouniverse.pay.internal.*` |

Rules:

- Do **not** expose `internal` types on public method signatures or Javadoc examples meant for integrators.
- Request/response **wire serialization** (`toJson` / `fromJson`) is **package-private** on payment types — only `PaymentsClient` and tests in the same package should call them.
- **`Canonicalizer.canonicalizeJson(String)`** is the supported public helper for signature debugging.

Sub-clients:

- `client.payments()` → `init`, `query`, `reverse`, `refund`
- `client.webhooks()` → `parse` (same Presto public key as the client)

Paths (always under configured base URL):

| Method | Path | Idempotent retry in pipeline |
|--------|------|------------------------------|
| init | `/v1/ext/payment/init` | Only if `requestNotSent()` |
| query | `/v1/ext/payment/query` | Yes (read-only) |
| reverse | `/v1/ext/payment/reverse` | Only if `requestNotSent()` |
| refund | `/v1/ext/payment/refund` | Only if `requestNotSent()` |

Pipeline implementation: `internal.RequestPipeline` (injects `mid`, `prestoMrn`, timestamp, signature, verifies response signature).

## Type and naming conventions

Follow existing patterns in `payments/` and `webhooks/`:

1. **Operations** use `*Request` immutable builders (e.g. `PaymentInitRequest.builder()`), not `*Params`.
2. **Gateway code lists** use **`public static final String`** holder classes (no Java `enum` for open-ended gateway strings):
   - `PaymentStatus`, `RefundStatus`, `ReversalStatus`, `PaymentMethod`, `TxnType`, `ErrorCode`
   - Response getters return **`String`** for those wire values; integrators compare to constants or handle unknown gateway values as strings.
3. **`NotifyEventCode`** is a **`String` constant holder** (same pattern as `PaymentStatus`). Use `NotifyEvent.getPaymentStatus()` after parse; `payments().query()` remains authoritative.
4. **Validation** lives in `payments.Validation` and request `build()` methods; throw `PrestoPayConfigException` for client-side validation failures.
5. **Errors:** unchecked `PrestoPayException` subclasses — see README error table.

When adding a new gateway-known code, add a constant to the appropriate holder class and extend contract tests if behavior is user-visible.

## Signing and JSON (implementation notes)

Agents editing crypto or JSON must preserve gateway behavior:

- Canonical string: sort body keys (excluding `signature`), concatenate **values only** with `:`, null → empty string, then `SHA256withRSA`, Base64 signature.
- JSON is **hand-rolled** under `internal.json` — no Gson/Jackson dependency. Unicode and control-character round-trips have dedicated tests.
- Timestamps use UTC+8 rules via `internal.Timestamps` / pipeline clock (`Clock` injectable for tests).

## Retry and idempotency (do not regress)

Documented in README and `PrestoPayClient` Javadoc:

- **`init` / `reverse` / `refund`:** unsafe to retry after the HTTP request may have reached Presto. Default policy retries only when `PrestoPayTransportException.requestNotSent()` is true.
- Duplicate `txnRefNum` on init → gateway error **`1203`** (`ErrorCode.DUPLICATE_TXN_REF_NUM`). After ambiguous init failure, integrators must **`query`** by `txnRefNum`, not re-init.
- **`query`** may retry on transport errors and retryable API failures.

Any change to `RetryPolicy`, `RequestPipeline`, or `PaymentsClient` idempotent flags requires updating `RetryPolicyContractTest` and README if behavior changes.

## Testing expectations

- Prefer **contract tests** with `support.MockGatewayServer` over live gateway calls.
- `StagingSmokeTest` is `@Tag("staging")` — excluded unless `-Pstaging-smoke`.
- After substantive changes, run **`mvn verify`** locally.
- New public behavior: add or extend tests in the matching package; avoid trivial assertions.

## Code style and constraints

- **Java 8** language level (`maven.compiler.release=8`) — no newer language features.
- **Checkstyle** runs at `validate` and `verify` — fix violations; suppressions live in `config/checkstyle/suppressions.xml` (use sparingly).
- **Zero new runtime Maven dependencies** unless explicitly requested — product goal is zero-deps.
- Keep diffs minimal; match surrounding naming and builder patterns.
- Update [CHANGELOG.md](CHANGELOG.md) for user-visible API or behavior changes when preparing releases.
- Do **not** commit secrets (`.p12`, passwords, live credentials). Staging smoke uses environment variables only.

## Common agent tasks

| Task | Where to look |
|------|----------------|
| New payment field | Matching `*Request` / response, `Validation`, contract test JSON |
| New payment method or status constant | `PaymentMethod` / `PaymentStatus` (etc.), README if integrator-facing |
| Spring Boot integrator example | `sample/spring-boot-payment-demo/` |
| HTTP observability / proxy | Implement `HttpTransport`, wire in `PrestoPayClient.Builder` |
| Webhook replay window | `WebhookVerifier.Builder.maxTimestampAge` |
| Version string | `src/main/resources/presto-pay-sdk-version.properties`, `SdkVersion` |

## References

- [README.md](README.md) — integrator quick start, env vars, errors
- [docs/spring-wiring.md](docs/spring-wiring.md) — Spring Boot wiring
- [CHANGELOG.md](CHANGELOG.md) — release notes

## Out of scope unless asked

- Maven Central publish pipeline (GPG, central publishing plugin)
- `/ext/user/verify` mini-app user token flow
- Framework-specific modules (Spring starter, Quarkus, etc.) beyond the doc example

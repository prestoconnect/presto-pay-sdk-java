# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Changed

- **Breaking:** constants in `NotifyEventCode`, `PaymentMethod`, `PaymentStatus`, `RefundStatus`, `ReversalStatus`, and `TxnType` are renamed so each name matches its gateway string value (e.g. `PaymentStatus.PENDING_AUTHORISE` → `PaymentStatus.PendingAuthorise`, `TxnType.WEB_PAY` → `TxnType.WebPay`, `PaymentMethod.PM_PG_CARD` → `PaymentMethod.PmPgCard`). Values are unchanged. `ErrorCode` keeps descriptive names because its values are numeric.

## [0.1.0] - 2026-09-23

First public release.

### Added

- README with integration guide, error handling, and webhook contract.
- Apache 2.0 LICENSE file.
- GitHub Actions CI running `mvn verify`.
- `SdkVersion` and JAR `Implementation-Version` for accurate `User-Agent` headers.
- `Canonicalizer.canonicalizeJson(String)` for debugging without using internal JSON types.
- Opt-in staging smoke test (`PRESTOPAY_STAGING_SMOKE=1`, `mvn verify -Pstaging-smoke`).
- Package-level Javadoc on the root `com.prestouniverse.pay` package.
- `PrestoPayResponseException` (with `source()` and `rawBody()`) for response and webhook bodies that cannot be parsed.
- `WebhookVerifier.Builder.merchantId(...)` (required): webhooks for any other `mid` are rejected. Presto signs all partners' webhooks with the same key, so a genuine event for another merchant previously verified.
- `PrestoPayClient.merchantId()` accessor.
- `HttpTransport.jdkDefault()` for wrapping the built-in transport, and Javadoc on the `HttpTransport` contract (report `requestNotSent` accurately, never resend, do not follow redirects).
- `PrestoPayKeys.privateKeyFromPkcs12(path|stream, password)` overloads that use the keystore's only private key; `PRESTOPAY_KEYSTORE_ALIAS` is now optional.
- `PaymentInitRequest.Builder.items(List)` and `allowedPaymentMethods(List)` overloads.
- `toString()` on response types, `NotifyEvent`, `PaymentDetail`, and `RefundDetail` showing identifiers, status, and amounts only (no user refs, additional data, or card details).
- CI runs on JDK 8, 11, 17, and 21 and builds the Javadoc jar.
- Webhooks are rejected when their signed `ts` is more than 15 minutes from the local clock (`WebhookVerifier.DEFAULT_MAX_TIMESTAMP_AGE`); configure with `WebhookVerifier.Builder.maxTimestampAge(...)` / `disableTimestampCheck()` or `PrestoPayClient.Builder.webhookMaxTimestampAge(...)`.
- Javadoc for all public types and packages; `SECURITY.md`.
- Maven wrapper (Maven 3.9.9) and a tag-triggered `Release` workflow that signs and uploads to Maven Central.

### Fixed

- Default HTTP transport could silently resend a POST (`init`, `reverse`, `refund`) when a keep-alive connection dropped; it now uses fixed-length streaming, does not follow redirects, and treats everything after connect as possibly sent.
- Malformed or incomplete response and webhook bodies (missing fields, unsupported value types, unparseable `ts`) now throw `PrestoPayResponseException` instead of raw `IllegalArgumentException` / `DateTimeParseException`.
- Integers outside `int` range are rejected instead of silently truncated.
- JSON nesting is limited to 32 levels so untrusted webhook bodies cannot exhaust the stack.
- Client builder rejects null, zero, sub-millisecond, and over-`Integer.MAX_VALUE` ms timeouts (zero meant "wait forever"), and null `retryPolicy` / `clock`.
- A `PKCS#12` alias that is not a private key now throws `PrestoPayConfigException` instead of `ClassCastException`.
- `baseUrl(...)` ignores trailing slashes (previously produced `//v1/...` paths) and rejects values without an `http(s)://` scheme.
- `PaymentInitRequest` and `PaymentRefundRequest` reject a non-positive `amount`; `PaymentRefundRequest` enforces the 255-character `notifyUrl` limit; list setters accept `null` as empty and reject `null` elements.
- The project now compiles on JDK 8 (`maven.compiler.release` is only set on JDK 9+).

### Changed

- Renamed payment builders from `*Params` to `*Request` (`PaymentInitRequest`, `PaymentQueryRequest`, etc.).
- `*Request#toJson()` and `*Response#fromJson()` are no longer public API (package-private).
- Malformed gateway response bodies throw `PrestoPayResponseException` instead of `PrestoPaySignatureException`; signature exceptions now only mean authenticity failures.
- `PaymentInitResponse` only requires `paymentRefNum` and `paymentStatus`; other fields may be `null`.
- `amount()` on `PaymentInitResponse`, `PaymentQueryResponse`, `PaymentReverseResponse`, and `amount()` / `refundAmount()` on `PaymentRefundResponse` return `Integer` (`null` when omitted) instead of `int` defaulting to `0`.
- One `mid` per client: `PrestoPayClient.Builder.merchantId(...)` / `PRESTOPAY_MID` is required and sent on every request, and `client.webhooks().parse` rejects events for any other `mid`. `*Request` builders no longer have `merchantId(...)`. To serve several merchants, build one client per `mid`.
- `prestoMrn` is per request: `PrestoPayClient.Builder.merchantRefNum(...)` and `PRESTOPAY_MRN` are gone; every `*Request` must set `merchantRefNum`, and `build()` rejects missing or blank values.
- Renamed `NotifyEvent.getPaymentStatus()` to `paymentStatus()` to match the other accessors.
- `PaymentsClient` can no longer be constructed directly (its constructor exposed an internal type); use `client.payments()`. `PaymentDetail.parseList` / `RefundDetail.parseList` and `Canonicalizer.canonicalize(JsonObject)` are no longer public; `Canonicalizer.canonicalizeJson(String)` remains.
- Removed `Environment.custom(String)`; use `PrestoPayClient.Builder.baseUrl(...)`.
- `RetryPolicy.of` throws `PrestoPayConfigException` (was `IllegalArgumentException`) and rejects a null or negative backoff.
- Lists returned by `PaymentQueryResponse` and `NotifyEvent` are unmodifiable.

[Unreleased]: https://github.com/prestouniverse/presto-pay-sdk/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/prestouniverse/presto-pay-sdk/releases/tag/v0.1.0

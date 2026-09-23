# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added

- README with integration guide, error handling, and webhook contract.
- Apache 2.0 LICENSE file.
- GitHub Actions CI running `mvn verify`.
- `SdkVersion` and JAR `Implementation-Version` for accurate `User-Agent` headers.
- `Canonicalizer.canonicalizeJson(String)` for debugging without using internal JSON types.
- Opt-in staging smoke test (`PRESTOPAY_STAGING_SMOKE=1`, `mvn verify -Pstaging-smoke`).
- Package-level Javadoc on the root `com.prestouniverse.pay` package.

### Changed

- Renamed payment builders from `*Params` to `*Request` (`PaymentInitRequest`, `PaymentQueryRequest`, etc.).
- `*Request#toJson()` and `*Response#fromJson()` are no longer public API (package-private).

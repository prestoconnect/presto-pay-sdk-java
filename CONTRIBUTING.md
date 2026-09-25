# Contributing

Thanks for looking at `presto-pay-sdk`. This covers building, testing, and releasing the library itself —
for how to *use* the SDK in your own project, see [README.md](README.md).

## Building and testing

The Maven wrapper pins Maven 3.9.9 (use `mvnw.cmd` on Windows):

```bash
./mvnw verify
```

CI (`.github/workflows/ci.yml`) runs this on JDK 8, 11, 17, and 21. Checkstyle only runs on JDK 11+ (its own
classes need Java 11+ to load).

Optional live staging smoke test (requires real staging credentials):

```bash
export PRESTOPAY_STAGING_SMOKE=1
# plus PRESTOPAY_ENV, PRESTOPAY_MID, PRESTOPAY_MRN, keystore, and public key variables
./mvnw verify -Pstaging-smoke
```

Release artifacts (sources + Javadoc): `./mvnw verify -Prelease`.

## Code style

- Java 8 language level (`maven.compiler.release=8`) — no newer language features.
- Checkstyle runs at `validate` and `verify`; suppressions live in `config/checkstyle/suppressions.xml`
  (use sparingly).
- Zero new runtime Maven dependencies unless explicitly agreed — the product goal is zero-deps.
- Match existing naming and builder patterns in `payments/` and `webhooks/`. See [AGENTS.md](AGENTS.md) for
  the full set of conventions this codebase follows.

## Releasing

1. Set `<version>` in `pom.xml` (no `-SNAPSHOT`) and move the CHANGELOG `Unreleased` entries under that
   version.
2. Commit, then push a matching tag, e.g. `git tag v0.1.0 && git push origin v0.1.0`.
3. The `Release` workflow verifies, signs, and uploads to the Maven Central Portal. The deployment waits
   there until someone clicks **Publish** (set `central.autoPublish=true` to skip that).
4. Bump `pom.xml` to the next `-SNAPSHOT` version.

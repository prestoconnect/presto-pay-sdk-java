# Copilot instructions

Follow the repository agent guide: [AGENTS.md](../AGENTS.md).

Summary: Java 8 Maven SDK, zero runtime dependencies, Checkstyle + `mvn verify`. Public API excludes `com.prestouniverse.pay.internal`. Gateway codes are `String` constants; payment types use `*Request` builders. Do not weaken init/refund/reverse idempotency or duplicate-`txnRefNum` safety.

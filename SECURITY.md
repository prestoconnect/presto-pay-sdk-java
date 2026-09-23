# Security policy

## Supported versions

Security fixes are released for the latest minor version of `presto-pay-sdk`.

| Version | Supported |
|---------|-----------|
| 0.1.x   | Yes       |

## Reporting a vulnerability

Please **do not** open a public issue for security problems.

Report privately through [GitHub private vulnerability reporting](https://github.com/prestouniverse/presto-pay-sdk/security/advisories/new). Include:

- the SDK version and Java version;
- what an attacker could do, and under which configuration;
- steps or a minimal code sample to reproduce.

We aim to acknowledge reports within 3 business days and to agree on a fix and disclosure timeline with you. Please give us a reasonable chance to release a fix before disclosing publicly.

## Scope

In scope: request signing, response and webhook verification, key loading, the default HTTP transport, and anything in this repository that could let an attacker forge, replay, or redirect payment requests or notifications.

Out of scope: the Presto Connect gateway itself (report those to Presto directly), and the sample application under `sample/`, which is for local demonstration only.

## Handling credentials

Never commit PKCS#12 keystores, passwords, or production merchant identifiers. The keys under `src/test/resources/keys/` are throwaway test keys generated for this repository's unit tests.

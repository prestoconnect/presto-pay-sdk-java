# Staging signing keys (classpath)

Add these files **here** (`src/main/resources/keys/`) so they are on the classpath at `keys/*`:

| File | Purpose |
|------|---------|
| `presto_rm_keystore.p12` | Partner private key for request signing (PKCS#12) |
| `presto_ext_service_dev.der` | Presto staging public key for response/webhook verification |

Defaults in `application.yml`:

- `prestopay.keystore-path`: `classpath:keys/presto_rm_keystore.p12`
- `prestopay.public-key-path`: `classpath:keys/presto_ext_service_dev.der`
- Password: `123123123`, alias: `rm`

`.p12` / `.der` files are gitignored; copy them from your Presto onboarding pack.

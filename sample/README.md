# Samples

## Spring Boot payment demo

[`spring-boot-payment-demo/`](spring-boot-payment-demo/) — two checkout UIs against **Presto staging** with the 11Street mock merchant.

### Staging credentials (defaults)

| Setting | Value |
|---------|--------|
| Environment | `staging` |
| `mid` | `11StreetMock` |
| `prestoMrn` | `PM181019QGJWH4K` |
| Keystore | `classpath:keys/presto_rm_keystore.p12` (password `123123123`, alias `rm`) |
| Presto public key | `classpath:keys/presto_ext_service_dev.der` |

Copy the `.p12` and `.der` files into [`src/main/resources/keys/`](spring-boot-payment-demo/src/main/resources/keys/) — see [`keys/README.md`](spring-boot-payment-demo/src/main/resources/keys/README.md). They are gitignored; obtain them from your Presto onboarding pack.

Override any value with `PRESTOPAY_*` environment variables or `application-local.yml`. If **all** of `PRESTOPAY_MID`, `PRESTOPAY_KEYSTORE_PATH`, `PRESTOPAY_KEYSTORE_PASSWORD`, and `PRESTOPAY_PUBLIC_KEY_PATH` are set, the app uses `PrestoPayClient.fromEnv()` instead of `application.yml`.

### Checkout UIs

| Route | Experience | SDK |
|-------|------------|-----|
| [`/hosted`](http://localhost:8080/hosted) | Amount + description → Presto hosted page | `init` without `allowedPaymentMethods` |
| [`/self-hosted`](http://localhost:8080/self-hosted) | Branded UI + payment method tiles | `init` with `allowedPaymentMethods(...)` |

### Source layout

```
com.prestouniverse.pay.sample.demo
├── PaymentDemoApplication.java
├── config/              AppProperties, PrestoPayProperties, PrestoPayConfiguration
├── controller/          Hosted / self-hosted / return / webhook
│   ├── handler/         CheckoutExceptionHandler
│   └── support/         CheckoutViewAttributes, PaymentInitRedirect
├── model/checkout/      Forms, CheckoutFlow, PaymentMethodCatalog
├── service/             WebPayCheckoutService
│   └── support/         CheckoutAmounts, DemoTxnReferenceGenerator
└── repository/          PaymentActivityStore
```

### Requirements

- **Java 8+** (aligned with `presto-pay-sdk`; sample uses Spring Boot 2.7)
- JDK 8 or newer to build and run

### Run

```bash
mvn install   # repo root
cd sample/spring-boot-payment-demo
# add keys/*.p12 and keys/*.der
export APP_PUBLIC_BASE_URL=https://your-ngrok-url   # for webhooks + redirect
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

### Webhooks (`notifyUrl`)

Each payment `init` sends `notifyUrl` = `{APP_PUBLIC_BASE_URL}/presto/notify`. Presto’s servers POST async events to that URL from **outside your network**. If the URL is not publicly reachable (for example `http://localhost:8080/...`), **webhooks will not arrive** — payments may still complete, but this demo’s “Recent webhooks” list and any server-side logic driven by notify will stay empty.

Use a tunnel or deployed host and set `APP_PUBLIC_BASE_URL` to that origin (HTTPS recommended). The payer `redirectUrl` should use the same base so return links work in the browser.

Each init sets `redirectUrl` to `{base}/return/{merchantTxnRef}`. The `/return/{txnRefNum}` handler **requires** that path segment; bare `/return` shows an error page.

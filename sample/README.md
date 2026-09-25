# Samples

## MyStore

[`my-store/`](my-store/) — a **MyStore**-branded checkout page against
**Presto staging**, styled with Tailwind CSS (Play CDN) and Font Awesome icons.

### Staging credentials

Use the staging merchant credentials from your Presto onboarding pack. Nothing secret ships with the sample.

1. Copy the `.p12` and `.der` files into [`src/main/resources/keys/`](my-store/src/main/resources/keys/) — see [`keys/README.md`](my-store/src/main/resources/keys/README.md). They are gitignored.
2. Create `my-store/application-local.yml` (gitignored) with your values:

```yaml
prestopay:
  mid: YOUR_STAGING_MID
  mrn: YOUR_STAGING_PRESTO_MRN
  keystore-password: YOUR_KEYSTORE_PASSWORD
  # keystore-alias: only if the keystore holds more than one private key
```

| Setting | Default |
|---------|---------|
| Environment | `staging` |
| Keystore | `classpath:keys/presto_rm_keystore.p12` |
| Presto public key | `classpath:keys/presto_ext_service_dev.der` |

Any value can also come from `PRESTOPAY_*` environment variables. If **all** of `PRESTOPAY_MID`, `PRESTOPAY_KEYSTORE_PATH`, `PRESTOPAY_KEYSTORE_PASSWORD`, and `PRESTOPAY_PUBLIC_KEY_PATH` are set, the app uses `PrestoPayClient.fromEnv()` instead of `application.yml`.

### Checkout UI

One page ([`/`](http://localhost:8080/), `HomeController`) with a **"Show payment methods on checkout"**
toggle that switches between the two ways to call `init`, both posting to the same `POST /checkout`:

| Toggle | Experience | SDK |
|--------|------------|-----|
| Off (default) | Amount + description → Presto hosted page | `init` without `allowedPaymentMethods` |
| On | Payment method list shown on this page first | `init` with `allowedPaymentMethods(...)` |

`GET /return/{txnRefNum}` shows the payment result (queries `payments().query()` for authoritative status).

### Source layout

```
com.prestouniverse.pay.sample.demo
├── PaymentDemoApplication.java
├── config/              AppProperties, PrestoPayProperties, PrestoPayConfiguration
├── controller/          HomeController (GET / and POST /checkout), ReturnController, WebhookController
│   ├── handler/         CheckoutExceptionHandler
│   └── support/         CheckoutViewAttributes, PaymentInitRedirect
├── model/checkout/      CheckoutForm, CheckoutFlow, PaymentMethodCatalog
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
cd sample/my-store
# add keys/*.p12 and keys/*.der
export APP_PUBLIC_BASE_URL=https://your-ngrok-url   # for webhooks + redirect
mvn spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

### Webhooks (`notifyUrl`)

Each payment `init` sends `notifyUrl` = `{APP_PUBLIC_BASE_URL}/presto/notify`. Presto’s servers POST async events to that URL from **outside your network**. If the URL is not publicly reachable (for example `http://localhost:8080/...`), **webhooks will not arrive** — payments may still complete, but this demo’s “Recent webhooks” list and any server-side logic driven by notify will stay empty.

Use a tunnel or deployed host and set `APP_PUBLIC_BASE_URL` to that origin (HTTPS recommended). The payer `redirectUrl` should use the same base so return links work in the browser.

Each init sets `redirectUrl` to `{base}/return/{merchantTxnRef}`. The `/return/{txnRefNum}` handler **requires** that path segment; bare `/return` shows an error page.

## Custom HttpTransport reference

[`custom-transport/`](custom-transport/) — reference `HttpTransport` implementations backed by
Spring's `RestClient`, the JDK 11+ `HttpClient`, and OkHttp, for integrators who want to reuse an HTTP
client they already depend on instead of the SDK's default `HttpURLConnection`-based transport. Copy the
class you need out of this module rather than depending on it. See its own
[README](custom-transport/README.md) for the `HttpTransport` contract each implementation follows.

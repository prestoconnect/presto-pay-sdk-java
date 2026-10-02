# Presto Pay SDK for Java

[![Maven Central](https://img.shields.io/maven-central/v/com.prestouniverse/presto-pay-sdk.svg)](https://central.sonatype.com/artifact/com.prestouniverse/presto-pay-sdk)
[![CI](https://github.com/prestoconnect/presto-pay-sdk-java/actions/workflows/ci.yml/badge.svg)](https://github.com/prestoconnect/presto-pay-sdk-java/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/license-Apache%202.0-blue.svg)](LICENSE)

Accept payments through the **Presto Connect** payment gateway from any Java application. The SDK signs every
request, verifies every response and webhook, and gives you typed requests and results, so you don't have to
handle the gateway's signature scheme yourself.

- **Java 8+**, no framework required
- **Zero runtime dependencies**
- **Thread-safe** `PrestoPayClient`: build one and share it

## Contents

- [Install](#install)
- [Before you start](#before-you-start)
- [How a payment works](#how-a-payment-works)
- [Quick start](#quick-start)
- [Payment statuses](#payment-statuses)
- [Next steps](#next-steps)

## Install

Maven:

```xml
<dependency>
  <groupId>com.prestouniverse</groupId>
  <artifactId>presto-pay-sdk</artifactId>
  <version>0.2.1</version>
</dependency>
```

Gradle: `implementation("com.prestouniverse:presto-pay-sdk:0.2.1")`

## Before you start

### 1. Create your key pair

You sign every request with your own RSA private key, and Presto verifies it with the matching public key.
Generate the pair yourself; the private key never leaves your systems. `keytool` ships with the JDK:

```bash
keytool -genkeypair -alias merchant -keyalg RSA -keysize 2048 -validity 99999 \
  -dname "CN=Your Company" -storetype PKCS12 -keystore merchant.p12
keytool -exportcert -alias merchant -keystore merchant.p12 -file merchant.der
```

`merchant.p12` holds your private key; keep it and its password secret, and out of source control. Send
`merchant.der` (your public key, in the DER format Presto requires) to Presto.
The certificate is valid for 99999 days (until the year 2300), so you won't have to generate a new key pair
and register it with Presto again.

### 2. Get your details from Presto

| From Presto | What it is | Where it goes |
|-------------|------------|---------------|
| Merchant ID (`mid`) | Identifies your merchant account | `PrestoPayClient.builder().merchantId(...)` |
| Presto merchant reference (`prestoMrn`) | Identifies the shop or outlet; one `mid` can have several | Every request: `merchantRefNum(...)` |
| Presto certificate (`.der`) | Verifies Presto's responses and webhooks; the SDK reads it as is | `PrestoPayClient.builder().prestoPublicKey(...)` |

Staging and production are separate: each has its own `mid`, `prestoMrn` and Presto certificate, and you
register your public key for each. Never mix them.

## How a payment works

```
 Your server                      Presto                     Shopper's browser
     |---- 1. init ------------------>|                              |
     |<--- paymentUrl ----------------|                              |
     |---- 2. redirect to paymentUrl ------------------------------->|
     |                                |<---- 3. shopper pays --------|
     |                                |---- 4a. redirect to your redirectUrl -->|
     |<--- 4b. webhook to your notifyUrl                             |
     |---- 5. query ----------------->|                              |
```

1. Your server calls `init` with your order's reference and amount. Presto returns a `paymentUrl`.
2. You redirect the shopper to `paymentUrl`.
3. The shopper chooses a payment method and pays on Presto's page.
4. Presto sends the shopper's browser back to your `redirectUrl` **and** POSTs a signed webhook to your
   `notifyUrl`. These happen independently and can arrive in either order.
5. On both, you call `query` to get the payment's status from Presto, and update the order.

The identifiers you'll see:

| Name | Who creates it | What it's for |
|------|----------------|---------------|
| `txnRefNum` | You | Your reference for the payment, such as an order ID. Unique per payment, at most 50 characters |
| `paymentRefNum` | Presto | Presto's reference for the payment, returned by `init` |
| `eventRefNum` | Presto | Identifies one webhook event; stays the same when Presto redelivers it |
| `reversalRefNum`, `refundRefNum` | You | Your reference for a reversal or a refund |

## Quick start

### 1. Create the client

Build it once at startup and reuse it. Bad keys or a wrong password fail here, not on the first payment.

```java
import com.prestouniverse.pay.Environment;
import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.PrestoPayKeys;
import java.nio.file.Paths;

PrestoPayClient presto = PrestoPayClient.builder()
    .environment(Environment.STAGING)
    .merchantId("YOUR_MID")
    .privateKey(PrestoPayKeys.privateKeyFromPkcs12(Paths.get("merchant.p12"), keystorePassword))
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get("presto.der")))
    .build();
```

`keystorePassword` is a `char[]`, for example from your secret store. To configure the client from environment
variables instead, see [Configuration](docs/production.md#configuration-from-environment).

### 2. Start a payment

```java
import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.payments.TxnType;

PaymentInitResponse payment = presto.payments().init(PaymentInitRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN")
    .txnType(TxnType.WebPay)
    .txnRefNum(orderId)
    .displayDesc("Order " + orderId)
    .amount(10_000) // minor units: MYR 100.00
    .currencyCode("MYR")
    .notifyUrl("https://your-app.example/presto/notify")
    .redirectUrl("https://your-app.example/presto/return/" + orderId)
    .allowedPaymentMethods(PaymentMethod.PmPgCard) // Skip this unless you build your own payment selection page
    .build());

// Save payment.paymentRefNum() with the order, then send the shopper to Presto.
response.sendRedirect(payment.paymentUrl());
```

`notifyUrl` must be reachable from the internet; on your own machine, use a tunnel such as ngrok. For the codes
you can pass to `allowedPaymentMethods`, see [Payment methods](docs/payment-methods.md).

### 3. Show the result on your return page

The redirect only tells you the shopper came back, not whether they paid. Ask Presto:

```java
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.payments.PaymentStatus;

PaymentQueryResponse result = presto.payments().query(PaymentQueryRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN")
    .txnRefNum(orderId)
    .build());

if (PaymentStatus.Authorised.equals(result.paymentStatus())) {
    // Paid: show the confirmation.
} else if (PaymentStatus.PendingAuthorise.equals(result.paymentStatus())) {
    // Not finished yet: show "processing" and check again shortly.
} else {
    // Not paid (Failed, Cancelled, Expired, ...).
}
```

### 4. Handle the webhook

A webhook tells you something happened to a payment (`eventCode`, and `success` for whether it worked), not
the payment's resulting status, so query for that here too. Verify the **raw** request body, exactly as
received. This example uses Spring; any framework works the same way.

```java
import com.prestouniverse.pay.exception.PrestoPayException;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.webhooks.NotifyAck;
import com.prestouniverse.pay.webhooks.NotifyEvent;

@PostMapping(value = "/presto/notify", consumes = "application/json")
public ResponseEntity<String> notify(@RequestBody String rawBody) {
    NotifyEvent event;
    try {
        event = presto.webhooks().parse(rawBody);
    } catch (PrestoPaySignatureException e) {
        return ResponseEntity.status(401).build(); // forged, for another mid, or too old
    } catch (PrestoPayResponseException e) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NotifyAck.ok()); // malformed body
    }

    PaymentQueryResponse payment;
    try {
        payment = presto.payments().query(PaymentQueryRequest.builder()
            .merchantRefNum(event.prestoMrn())
            .paymentRefNum(event.paymentRefNum())
            .build());
    } catch (PrestoPayException e) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NotifyAck.resend());
    }
    orders.applyStatus(event.txnRefNum(), payment.paymentStatus());
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NotifyAck.ok());
}
```

`NotifyAck.ok()` tells Presto the event is handled. `NotifyAck.resend()` asks Presto to deliver it again (after
1, 2, 5 and 10 minutes), which you want when your own processing failed.

The same event can arrive more than once, so `applyStatus` checks the order, not the event: it finalises the
order only if the order hasn't been finalised yet, and fulfils only on the change into `Authorised`. A
redelivery then finds the order already in that status and changes nothing. See
[Webhooks](docs/webhooks.md#handling-redeliveries) for the details.

Update the order the same way from your return page and your webhook: whichever arrives first records the
status, and the other finds it already done.

## Payment statuses

`paymentStatus()` returns one of these strings; compare it with the `PaymentStatus` constants.

| Status | Meaning | What to do |
|--------|---------|------------|
| `PendingAuthorise` | Created; the shopper hasn't finished paying | Wait. It becomes `Expired` if not paid within 15 minutes of `init` |
| `Authorised` | Paid | Fulfil the order |
| `Failed` | The payment attempt failed | Don't fulfil |
| `Cancelled` | Cancelled before it was paid, for example by `reverse` | Don't fulfil |
| `Expired` | Not paid within 15 minutes | Don't fulfil; start a new payment if the shopper returns |
| `PendingReverse` | A reversal is in progress | Query again later |
| `Reversed` | The payment was reversed | Treat the order as cancelled |
| `PendingRefund` | A refund is in progress | Query again later |
| `PartialRefunded` | Part of the amount was refunded | Update the order's refunded amount |
| `Refunded` | The full amount was refunded | Treat the order as refunded |

The gateway can add statuses, so handle an unknown value without failing.

## Next steps

- [Payment methods](docs/payment-methods.md): every payment method code, which ones you can use, and passing a
  code the SDK doesn't list yet.
- [Payments and errors](docs/payments-and-errors.md): query, reverse and refund payments; handle errors and
  timeouts safely.
- [Webhooks](docs/webhooks.md): replies, redelivery, guarding the order update and the freshness window.
- [Production](docs/production.md): configuration, several merchants, custom HTTP clients, the go-live
  checklist and troubleshooting.
- [Samples](sample/README.md): a runnable Spring Boot checkout against Presto staging
  ([`my-store`](sample/my-store/)), and reference HTTP transports ([`custom-transport`](sample/custom-transport/)).

## Contributing

Building, testing, code style and the release process are in [CONTRIBUTING.md](CONTRIBUTING.md). Report
security issues as described in [SECURITY.md](SECURITY.md), not in a public issue.

## License

Apache License 2.0. See [LICENSE](LICENSE).

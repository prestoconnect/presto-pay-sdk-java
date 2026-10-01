# Payments and errors

This guide covers the four payment operations and how to handle their failures. It assumes you've set up a
client as in the [quick start](../README.md#quick-start).

- [Look up a payment](#look-up-a-payment)
- [Reverse a payment](#reverse-a-payment)
- [Refund a payment](#refund-a-payment)
- [Errors](#errors)
- [When you don't know whether it worked](#when-you-dont-know-whether-it-worked)
- [Retries and timeouts](#retries-and-timeouts)

Every request needs your `prestoMrn`, passed as `merchantRefNum(...)`. `build()` throws
`PrestoPayConfigException` if it, or any other required field, is missing.

## Look up a payment

`query` returns a payment's current status and details. Look it up by your `txnRefNum` or by Presto's
`paymentRefNum`:

```java
PaymentQueryResponse payment = presto.payments().query(PaymentQueryRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN")
    .txnRefNum("order-123") // or .paymentRefNum(...)
    .build());

payment.paymentStatus();  // see the status table in the README
payment.reversalStatus(); // Reversing, Failed or Success, once you've requested a reversal
payment.refundStatus();   // Refunding, Failed or Success, once you've requested a refund
payment.refundDetails();  // one entry per refund
```

`query` only reads, so it is always safe to call again.

## Reverse a payment

`reverse` undoes a whole payment:

```java
PaymentReverseResponse reversal = presto.payments().reverse(PaymentReverseRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN")
    .paymentRefNum(paymentRefNum)    // or .txnRefNum(...)
    .reversalRefNum("rev-order-123") // your reference for this reversal, at most 50 characters
    .remark("Customer cancelled")    // optional
    .notifyUrl("https://your-app.example/presto/notify") // optional: get a Reversed webhook
    .build());
```

What happens depends on the payment's status:

- **`PendingAuthorise`** (not paid yet): the payment is cancelled and its status becomes `Cancelled`.
- **`Expired`**: fails with error `1219` (`ErrorCode.INVALID_PAYMENT_STATUS_FOR_REVERSAL`). There's nothing left
  to undo.
- **Paid**: the gateway decides. It can refuse once the payment has settled (`1220`,
  `REVERSAL_NOT_ALLOWED_SETTLED`) or its reversal window has passed (`1221`, `REVERSAL_GRACE_PERIOD_ENDED`). Use
  `refund` instead in that case.

## Refund a payment

`refund` returns all or part of a paid payment's amount:

```java
PaymentRefundResponse refund = presto.payments().refund(PaymentRefundRequest.builder()
    .merchantRefNum("YOUR_PRESTO_MRN")
    .paymentRefNum(paymentRefNum)
    .refundRefNum("ref-order-123") // your reference for this refund, at most 50 characters
    .remark("Item out of stock")   // required, at most 200 characters
    .amount(2_500)                 // optional: leave it out to refund the full amount
    .notifyUrl("https://your-app.example/presto/notify") // optional: get a Refunded webhook
    .build());
```

You can request a refund for any payment method, but whether it succeeds depends on the method; some need
manual or offline processing by Presto. A successful `refund` call means Presto accepted the request, not
that the money has moved. Check `refundStatus()` with `query`, or wait for the `Refunded` webhook, before
treating the refund as complete. A refund on an unpaid (`PendingAuthorise`) payment fails with `1227`
(`INVALID_PAYMENT_STATUS_FOR_REFUND`); use `reverse` to cancel it instead.

## Errors

All SDK errors are unchecked and extend `PrestoPayException`:

| Type | When | Useful methods |
|------|------|----------------|
| `PrestoPayConfigException` | A builder or request is invalid, a key can't be loaded, or an environment variable is missing | `field()` |
| `PrestoPayTransportException` | Network failure, timeout or TLS error | `requestNotSent()` |
| `PrestoPayApiException` | Presto rejected the request: an HTTP error, or HTTP 200 with `success: false` | `errorCode()`, `errorMessage()`, `httpStatus()`, `isSystemError()`, `rawBody()` |
| `PrestoPaySignatureException` | A response or webhook signature is missing or invalid, a webhook is for another `mid`, or a webhook is too old | `side()`, `canonicalString()` |
| `PrestoPayResponseException` | A response or webhook body can't be parsed | `source()`, `rawBody()` |

Compare `errorCode()` with the `ErrorCode` constants, for example
`ErrorCode.PAYMENT_NOT_FOUND.equals(e.errorCode())`. For codes that point at your setup (`1005`, `1006`,
`1007`), see [Troubleshooting](production.md#troubleshooting).

## When you don't know whether it worked

A timeout, a server error or a garbled response on `init`, `reverse` or `refund` leaves you not knowing
whether Presto acted on the request. The SDK won't resend these for you, because doing so could reverse or
refund twice. Instead, ask Presto:

```java
try {
    PaymentInitResponse payment = presto.payments().init(request);
    response.sendRedirect(payment.paymentUrl());
} catch (PrestoPayTransportException e) {
    if (e.requestNotSent()) {
        throw e; // nothing reached Presto, so it's safe to try again
    }
    reconcile(orderId); // it may have been created
} catch (PrestoPayApiException e) {
    if (e.httpStatus() < 500) {
        throw e; // Presto refused the request
    }
    reconcile(orderId); // a server error: it may have been created
} catch (PrestoPayResponseException e) {
    reconcile(orderId); // Presto answered, so it may have been created
}
```

where `reconcile` is your code: it queries by the same `txnRefNum` and carries on from the status it finds. If
Presto has no such payment, it wasn't created and you can call `init` again. One refusal also calls for a query: `DUPLICATE_TXN_REF_NUM` (`1203`) means a payment
with that `txnRefNum` exists, but not what state it's in.

For `init`, calling it again with the same `txnRefNum` is also safe: Presto returns the existing payment and
its current status rather than creating a second one. After `reverse` or `refund`, query by `paymentRefNum` and
check `reversalStatus()` or `refundStatus()` before trying again.

## Retries and timeouts

The client retries for you only when it's safe:

- **`query`**: on network errors and HTTP 5xx responses.
- **`init`, `reverse`, `refund`**: only when the request certainly never left your machine, for example when
  the connection was refused or the host name didn't resolve (`requestNotSent()` is `true`).

By default the client retries twice, waiting 200 ms and then 400 ms, and waits up to 5 seconds to connect and
30 seconds for a response. To change this:

```java
PrestoPayClient presto = PrestoPayClient.builder()
    // ...
    .retryPolicy(RetryPolicy.of(3, Duration.ofMillis(500))) // or RetryPolicy.none()
    .connectTimeout(Duration.ofSeconds(3))
    .readTimeout(Duration.ofSeconds(20))
    .build();
```

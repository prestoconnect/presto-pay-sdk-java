# Webhooks

Presto POSTs a signed JSON webhook to the `notifyUrl` you pass to `init`, `reverse` or `refund` when something
happens to that payment. The [quick start](../README.md#4-handle-the-webhook) has a complete handler; this
guide explains each part.

- [What a webhook tells you](#what-a-webhook-tells-you)
- [Verifying it](#verifying-it)
- [Replying](#replying)
- [Handling redeliveries](#handling-redeliveries)
- [The freshness window](#the-freshness-window)
- [A webhook-only service](#a-webhook-only-service)

## What a webhook tells you

`presto.webhooks().parse(rawBody)` returns a `NotifyEvent`:

| Method | Value |
|--------|-------|
| `eventCode()` | What happened: `Authorised`, `Cancelled`, `Reversed`, `Refunded` or `Expired` (compare with `NotifyEventCode`). Presto may add codes |
| `success()` | Whether it worked. A `Refunded` event with `success()` false is a refund that failed |
| `txnRefNum()`, `paymentRefNum()`, `prestoMrn()` | Which payment it's about |
| `eventRefNum()` | Identifies this event; the same on every redelivery |
| `amount()`, `currencyCode()`, `paymentDetails()` | The payment's amount and how it was paid |

A webhook reports an event, not the payment's resulting status. A failed refund, for example, leaves the
payment in whatever status it had before, which the event doesn't carry. To act on a webhook, `query` the
payment and use the status it returns.

## Verifying it

`parse` checks, in order, that:

1. the body is signed by Presto,
2. the required fields are present,
3. the event is for your client's `mid`, and
4. its timestamp is within 15 minutes of your clock.

The `mid` check matters: Presto signs webhooks for every merchant with the same key, so a genuine webhook for
someone else's account would otherwise pass.

Pass the **raw** request body, exactly as received. If your framework parses the JSON for you and you
serialize it back, the signature won't match. In Spring, take the body as `@RequestBody String`. With the
Servlet API, read `request.getReader()` to the end.

`parse` throws `PrestoPaySignatureException` for a bad signature, another merchant's `mid`, or a stale
timestamp, and `PrestoPayResponseException` for a malformed body. Answer a `PrestoPaySignatureException` with HTTP
401. Answer a malformed body with HTTP 200 and `NotifyAck.ok()`, since a redelivery would fail the same way.
Never let either surface as a 500.

## Replying

Reply HTTP 200 with a JSON body:

| Body | Meaning | When to send it |
|------|---------|-----------------|
| `NotifyAck.ok()` (`{"resend":false}`) | Handled; don't send it again | You've updated the order, or it was already in that status |
| `NotifyAck.resend()` (`{"resend":true}`) | Send it again later | Your own processing failed, for example the `query` or your database |

Presto resends a notification with a backoff of 2, 4, 8, 16, 32, 64, 128, 256, 512 and 1024 minutes between
attempts, so an event is delivered at most 11 times over about 34 hours. Only ask for a resend when trying again
could succeed; never for a webhook that failed verification, which would fail the same way every time.

Reply quickly. Record the event and reply, and do slow work such as emails or fulfilment afterwards.

## Handling redeliveries

The same event can arrive more than once, for example after you ask for a resend, and your return page may
update the same order first. Guard on the order record rather than on the event:

- `query` the payment on every delivery, then apply its status to the order in one conditional update, so
  that only one caller can finalise it:

  ```sql
  UPDATE orders SET status = ? WHERE txn_ref_num = ? AND status = 'PendingAuthorise'
  ```

- fulfil only when that update changed a row and the new status is `Authorised`, and create the fulfilment
  job in the same transaction;
- once an order is finalised, apply only the statuses that can follow it (`PendingRefund`, `PartialRefunded`,
  `Refunded`, `PendingReverse`, `Reversed`), never fulfil again, and never let an older status overwrite a
  newer one;
- reply `NotifyAck.ok()` whether or not anything changed.

A redelivery, a replay, or a webhook that arrives after the return page then finds the order already in that
status and does nothing.

## The freshness window

`parse` rejects a webhook whose timestamp is more than 15 minutes from your clock, so a captured webhook can't
be replayed later. Each redelivery carries a fresh timestamp, so redeliveries pass. Keep your server's clock in
sync with NTP.

To change the window, set it when you build the client:

```java
PrestoPayClient presto = PrestoPayClient.builder()
    // ...
    .webhookMaxTimestampAge(Duration.ofMinutes(10))
    .build();
```

## A webhook-only service

A service that only receives webhooks doesn't need your private key. Build a `WebhookVerifier` with just
Presto's certificate and your `mid`:

```java
WebhookVerifier verifier = WebhookVerifier.builder()
    .prestoPublicKey(PrestoPayKeys.publicKeyFromX509(Paths.get("presto.der")))
    .merchantId("YOUR_MID")
    .build();

NotifyEvent event = verifier.parse(rawBody);
```

It applies the same checks as `presto.webhooks().parse`. `WebhookVerifier.Builder` also has `maxTimestampAge(...)`
and `disableTimestampCheck()`. Turn the check off only if your order update is guarded as described in
[Handling redeliveries](#handling-redeliveries), since that becomes your only protection against replays.

To receive webhooks for several merchants, give each one its own `notifyUrl`, for example
`/presto/notify/{mid}`, and verify each with that merchant's client or verifier.

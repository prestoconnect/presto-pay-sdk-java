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
timestamp, and `PrestoPayResponseException` for a malformed body. Answer those with HTTP 401 and HTTP 400, not
a 500.

## Replying

Reply HTTP 200 with a JSON body:

| Body | Meaning | When to send it |
|------|---------|-----------------|
| `NotifyAck.ok()` (`{"resend":false}`) | Handled; don't send it again | You've recorded the event, or had already recorded it earlier |
| `NotifyAck.resend()` (`{"resend":true}`) | Send it again later | Your own processing failed, for example the `query` or your database |

Presto retries 1, 2, 5 and 10 minutes after the first attempt, so an event is delivered at most five times over
about 18 minutes. Only ask for a resend when trying again could succeed; never for a webhook that failed
verification, which would fail the same way every time.

Reply quickly. Record the event and reply, and do slow work such as emails or fulfilment afterwards.

## Handling redeliveries

The same event can arrive more than once, for example after you ask for a resend. Every delivery of an event
has the same `eventRefNum`, so:

- record `eventRefNum` once you've handled the event, under a unique constraint in your database;
- skip events you've already recorded, and still reply `NotifyAck.ok()`;
- record it only after the `query` succeeds, so a failed attempt isn't mistaken for a handled one on redelivery.

Keep recorded `eventRefNum`s for at least as long as the redelivery schedule (about 18 minutes).

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
and `disableTimestampCheck()`. Turn the check off only if you deduplicate on `eventRefNum`, since that becomes
your only protection against replays.

To receive webhooks for several merchants, give each one its own `notifyUrl`, for example
`/presto/notify/{mid}`, and verify each with that merchant's client or verifier.

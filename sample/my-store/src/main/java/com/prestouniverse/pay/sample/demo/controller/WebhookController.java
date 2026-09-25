package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.webhooks.NotifyAck;
import com.prestouniverse.pay.webhooks.NotifyEvent;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore.WebhookRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final PrestoPayClient prestoPayClient;
    private final PaymentActivityStore activityStore;

    public WebhookController(PrestoPayClient prestoPayClient, PaymentActivityStore activityStore) {
        this.prestoPayClient = prestoPayClient;
        this.activityStore = activityStore;
    }

    @PostMapping(value = "/presto/notify", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> notify(@RequestBody String body) {
        final NotifyEvent event;
        try {
            event = prestoPayClient.webhooks().parse(body);
        } catch (PrestoPaySignatureException ex) {
            log.warn("Webhook rejected: signature verification failed side={} message={} canonical={}",
                    ex.side(), ex.getMessage(), ex.canonicalString());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        } catch (PrestoPayResponseException ex) {
            log.warn("Webhook rejected: malformed body message={}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        // event.paymentStatus() other than PaymentStatus.PendingAuthorise means Presto has finalised
        // this payment. This webhook and the payer's browser redirect to /return/{txnRefNum} are
        // triggered independently by Presto and can arrive in either order, or at nearly the same
        // time -- treat the update here as an idempotent upsert keyed by txnRefNum, not a step that
        // must happen before or after the return page loads.
        log.info("Webhook verified eventCode={} paymentStatus={} txnRefNum={} paymentRefNum={} success={} "
                        + "amount={} {} eventRefNum={}",
                event.eventCode(),
                event.paymentStatus(),
                event.txnRefNum(),
                event.paymentRefNum(),
                event.success(),
                event.amount(),
                event.currencyCode(),
                event.eventRefNum());

        activityStore.appendWebhook(new WebhookRecord(
                event.txnRefNum(),
                event.eventCode(),
                event.paymentStatus(),
                event.success(),
                event.amount(),
                event.currencyCode(),
                Instant.now()));

        log.debug("Webhook ack sent for txnRefNum={}", event.txnRefNum());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(NotifyAck.ok());
    }
}

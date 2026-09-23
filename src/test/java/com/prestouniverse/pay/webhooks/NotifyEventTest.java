package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NotifyEventTest {

    @Test
    void authorisedSuccessMapsToAuthorisedOrFailed() {
        assertEquals(PaymentStatus.Authorised, parse(NotifyEventCode.Authorised, true).paymentStatus());
        assertEquals(PaymentStatus.Failed, parse(NotifyEventCode.Authorised, false).paymentStatus());
    }

    @Test
    void knownEventCodesMapToMatchingPaymentStatus() {
        assertEquals(PaymentStatus.Cancelled, parse(NotifyEventCode.Cancelled, false).paymentStatus());
        assertEquals(PaymentStatus.Reversed, parse(NotifyEventCode.Reversed, true).paymentStatus());
        assertEquals(PaymentStatus.Refunded, parse(NotifyEventCode.Refunded, true).paymentStatus());
        assertEquals(PaymentStatus.Expired, parse(NotifyEventCode.Expired, false).paymentStatus());
    }

    @Test
    void unknownEventCodeIsPreserved() {
        NotifyEvent event = parse("FutureEvent", true);
        assertEquals("FutureEvent", event.eventCode());
        assertEquals("FutureEvent", event.paymentStatus());
    }

    @Test
    void toStringShowsIdentifiersButNotUserOrAdditionalData() {
        JsonObject body = body(NotifyEventCode.Authorised, true);
        body.put("userRefNum", "user-secret");
        body.put("additionalData", "extra-secret");
        body.put("paymentDetails", "[{\"method\":\"Card\",\"amount\":100,\"cardBin\":\"411111\",\"refNum\":\"R1\"}]");
        NotifyEvent event = NotifyEvent.fromJson(body);

        String text = event.toString() + event.paymentDetails();

        assertTrue(text.contains("paymentRefNum=PP1"));
        assertTrue(text.contains("refNum=R1"));
        assertFalse(text.contains("user-secret"));
        assertFalse(text.contains("extra-secret"));
        assertFalse(text.contains("411111"));
    }

    private static NotifyEvent parse(String eventCode, boolean success) {
        return NotifyEvent.fromJson(body(eventCode, success));
    }

    private static JsonObject body(String eventCode, boolean success) {
        JsonObject body = JsonCodec.newObject();
        body.put("eventCode", eventCode);
        body.put("mid", "MID");
        body.put("prestoMrn", "MRN");
        body.put("paymentRefNum", "PP1");
        body.put("txnRefNum", "TXN1");
        body.put("success", success);
        body.put("eventRefNum", "EV1");
        body.put("eventTs", "20250423093000.000");
        body.put("amount", 100);
        body.put("currencyCode", "MYR");
        body.put("paymentDetails", "[]");
        body.put("ts", "20250423093000.000");
        return body;
    }
}

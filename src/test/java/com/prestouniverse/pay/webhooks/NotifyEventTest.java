package com.prestouniverse.pay.webhooks;

import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.prestouniverse.pay.payments.PaymentStatus;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NotifyEventTest {

    @Test
    void authorisedSuccessMapsToAuthorisedOrFailed() {
        assertEquals(PaymentStatus.AUTHORISED, parse(NotifyEventCode.AUTHORISED, true).getPaymentStatus());
        assertEquals(PaymentStatus.FAILED, parse(NotifyEventCode.AUTHORISED, false).getPaymentStatus());
    }

    @Test
    void knownEventCodesMapToMatchingPaymentStatus() {
        assertEquals(PaymentStatus.CANCELLED, parse(NotifyEventCode.CANCELLED, false).getPaymentStatus());
        assertEquals(PaymentStatus.REVERSED, parse(NotifyEventCode.REVERSED, true).getPaymentStatus());
        assertEquals(PaymentStatus.REFUNDED, parse(NotifyEventCode.REFUNDED, true).getPaymentStatus());
        assertEquals(PaymentStatus.EXPIRED, parse(NotifyEventCode.EXPIRED, false).getPaymentStatus());
    }

    @Test
    void unknownEventCodeIsPreserved() {
        NotifyEvent event = parse("FutureEvent", true);
        assertEquals("FutureEvent", event.eventCode());
        assertEquals("FutureEvent", event.getPaymentStatus());
    }

    private static NotifyEvent parse(String eventCode, boolean success) {
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
        return NotifyEvent.fromJson(body);
    }
}

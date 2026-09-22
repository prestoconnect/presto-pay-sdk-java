package com.prestouniverse.pay.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenEnumTest {

    @Test
    void knownPaymentStatusParsesAsKnown() {
        PaymentStatus status = PaymentStatus.of("Authorised");
        assertEquals(PaymentStatus.AUTHORISED, status);
        assertTrue(status.isKnown());
    }

    @Test
    void unknownPaymentStatusParsesWithoutThrowing() {
        PaymentStatus status = PaymentStatus.of("SomeFutureStatus");
        assertEquals("SomeFutureStatus", status.value());
        assertFalse(status.isKnown());
    }

    @Test
    void unknownPaymentMethodParsesWithoutThrowing() {
        PaymentMethod method = PaymentMethod.of("SomeNewWallet");
        assertFalse(method.isKnown());
    }

    @Test
    void unknownTxnTypeParsesWithoutThrowing() {
        TxnType type = TxnType.of("SomeNewFlow");
        assertFalse(type.isKnown());
    }

    @Test
    void unknownRefundStatusParsesWithoutThrowing() {
        assertFalse(RefundStatus.of("Pending").isKnown());
    }

    @Test
    void unknownReversalStatusParsesWithoutThrowing() {
        assertFalse(ReversalStatus.of("Pending").isKnown());
    }
}

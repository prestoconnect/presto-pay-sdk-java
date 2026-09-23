package com.prestouniverse.pay.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenEnumTest {

    @Test
    void paymentMethodConstantsMatchGatewayValues() {
        assertEquals("Wallet", PaymentMethod.Wallet);
        assertEquals("GrabPayLater", PaymentMethod.GrabPayLater);
    }

    @Test
    void paymentStatusConstantsMatchGatewayValues() {
        assertEquals("Authorised", PaymentStatus.Authorised);
    }

    @Test
    void txnTypeConstantsMatchGatewayValues() {
        assertEquals("WebPay", TxnType.WebPay);
        assertEquals("QrPay", TxnType.QrPay);
    }
}

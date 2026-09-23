package com.prestouniverse.pay.payments;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OpenEnumTest {

    @Test
    void paymentMethodConstantsMatchGatewayValues() {
        assertEquals("Wallet", PaymentMethod.WALLET);
        assertEquals("GrabPayLater", PaymentMethod.GRAB_PAY_LATER);
    }

    @Test
    void paymentStatusConstantsMatchGatewayValues() {
        assertEquals("Authorised", PaymentStatus.AUTHORISED);
    }

    @Test
    void txnTypeConstantsMatchGatewayValues() {
        assertEquals("WebPay", TxnType.WEB_PAY);
        assertEquals("QrPay", TxnType.QR_PAY);
    }
}

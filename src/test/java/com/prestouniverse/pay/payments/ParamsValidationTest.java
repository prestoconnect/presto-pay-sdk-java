package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ParamsValidationTest {

    @Test
    void initRequiresTxnType() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentInitParams.builder()
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .build());
    }

    @Test
    void initRejectsQrValueAndPayerRefNumTogether() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentInitParams.builder()
                .txnType(TxnType.QR_PAY)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .qrValue("qr")
                .payerRefNum("payer")
                .build());
    }

    @Test
    void initRequiresCurrencyCodeWhenAmountIsSet() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentInitParams.builder()
                .txnType(TxnType.QR_PAY)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .amount(100)
                .build());
    }

    @Test
    void webPayRequiresRedirectUrl() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentInitParams.builder()
                .txnType(TxnType.WEB_PAY)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .build());
    }

    @Test
    void initRejectsTxnRefNumOverMaxLength() {
        String tooLong = repeat("a", 51);
        assertThrows(PrestoPayConfigException.class, () -> PaymentInitParams.builder()
                .txnType(TxnType.QR_PAY)
                .txnRefNum(tooLong)
                .displayDesc("desc")
                .build());
    }

    @Test
    void queryRequiresEitherRefNum() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentQueryParams.builder().build());
    }

    @Test
    void reverseRequiresReversalRefNum() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentReverseParams.builder()
                .paymentRefNum("PP1")
                .build());
    }

    @Test
    void refundRequiresRemark() {
        assertThrows(PrestoPayConfigException.class, () -> PaymentRefundParams.builder()
                .paymentRefNum("PP1")
                .refundRefNum("RFD1")
                .build());
    }

    private static String repeat(String s, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(s);
        }
        return builder.toString();
    }
}

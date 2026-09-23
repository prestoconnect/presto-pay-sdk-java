package com.prestouniverse.pay;

import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.payments.TxnType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live staging init with {@code allowedPaymentMethods}. Not run by default.
 *
 * <p>Run with {@code PRESTOPAY_STAGING_SMOKE=1}, {@link PrestoPayClient#fromEnv()} variables, and the test-only
 * {@code PRESTOPAY_MRN}.
 */
@Tag("staging")
@EnabledIfEnvironmentVariable(named = "PRESTOPAY_STAGING_SMOKE", matches = "1")
class AllowedPaymentMethodsStagingTest {

    private static final String NOTIFY = "http://localhost:8080/presto/notify";

    @Test
    void initWithSingleAllowedMethodUsesLocalhostUrls() {
        PrestoPayClient client = PrestoPayClient.fromEnv();
        String txnRefNum = "sdk-apm-" + System.currentTimeMillis();
        PaymentInitResponse response = client.payments().init(PaymentInitRequest.builder()
                .merchantRefNum(System.getenv("PRESTOPAY_MRN"))
                .txnType(TxnType.WEB_PAY)
                .txnRefNum(txnRefNum)
                .displayDesc("APM smoke Wallet")
                .amount(1000)
                .currencyCode("MYR")
                .notifyUrl(NOTIFY)
                .redirectUrl("http://localhost:8080/return?txnRefNum=" + txnRefNum)
                .allowedPaymentMethods(PaymentMethod.WALLET)
                .build());
        assertNotNull(response.paymentUrl());
    }
}

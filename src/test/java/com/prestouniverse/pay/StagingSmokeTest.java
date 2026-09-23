package com.prestouniverse.pay;

import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.payments.TxnType;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Live calls against {@link Environment#STAGING}. Not run by default.
 *
 * <p>Set {@code PRESTOPAY_STAGING_SMOKE=1} plus the same variables as {@link PrestoPayClient#fromEnv()},
 * then run {@code mvn verify -Pstaging-smoke}.
 */
@Tag("staging")
@EnabledIfEnvironmentVariable(named = "PRESTOPAY_STAGING_SMOKE", matches = "1")
class StagingSmokeTest {

    @Test
    void initThenQueryByTxnRefNum() {
        PrestoPayClient client = PrestoPayClient.fromEnv();
        String txnRefNum = "sdk-smoke-" + System.currentTimeMillis();

        PaymentInitResponse init = client.payments().init(PaymentInitRequest.builder()
                .txnType(TxnType.WEB_PAY)
                .txnRefNum(txnRefNum)
                .displayDesc("SDK staging smoke")
                .amount(100)
                .currencyCode("MYR")
                .notifyUrl("https://example.com/presto/notify")
                .redirectUrl("https://example.com/presto/return")
                .build());

        assertNotNull(init.paymentUrl());
        assertFalse(init.paymentUrl().isEmpty());

        PaymentQueryResponse query = client.payments().query(
                PaymentQueryRequest.builder().txnRefNum(txnRefNum).build());
        assertNotNull(query.txnRefNum());
    }
}

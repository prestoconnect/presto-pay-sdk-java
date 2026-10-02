package com.prestouniverse.pay.sample.demo.repository;

import com.prestouniverse.pay.payments.PaymentStatus;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore.StatusChange;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentActivityStoreTest {

    private final PaymentActivityStore store = new PaymentActivityStore();

    @Test
    void redeliveryAfterPaymentFulfilsOnlyOnce() {
        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Authorised)).isEqualTo(StatusChange.PAID);
        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Authorised)).isEqualTo(StatusChange.NONE);
    }

    @Test
    void refundAppliesToPaidOrderButStaleStatusCannotUndoIt() {
        store.applyPaymentStatus("order-1", PaymentStatus.Authorised);

        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Refunded)).isEqualTo(StatusChange.UPDATED);
        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Authorised)).isEqualTo(StatusChange.NONE);
    }

    @Test
    void failedRefundReturnsToAuthorisedWithoutFulfillingAgain() {
        store.applyPaymentStatus("order-1", PaymentStatus.Authorised);
        store.applyPaymentStatus("order-1", PaymentStatus.PendingRefund);

        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Authorised)).isEqualTo(StatusChange.UPDATED);
    }

    @Test
    void expiredOrderStaysExpired() {
        store.applyPaymentStatus("order-1", PaymentStatus.Expired);

        assertThat(store.applyPaymentStatus("order-1", PaymentStatus.Authorised)).isEqualTo(StatusChange.NONE);
    }
}

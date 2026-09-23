package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.payments.PaymentDetail;
import com.prestouniverse.pay.payments.PaymentsClient;

import java.util.List;

/**
 * Lets SDK packages reach each other's package-private factories without widening the public API.
 * The {@code payments} package registers its implementation from {@code PaymentsClient}'s static initializer.
 */
public final class SdkAccess {

    /** Package-private capabilities of {@code com.prestouniverse.pay.payments}. */
    public interface Payments {

        PaymentsClient newPaymentsClient(RequestPipeline pipeline);

        List<PaymentDetail> parsePaymentDetails(String json);
    }

    private static volatile Payments payments;

    private SdkAccess() {
    }

    public static synchronized void registerPayments(Payments implementation) {
        if (payments != null) {
            throw new IllegalStateException("Payments access is already registered");
        }
        payments = implementation;
    }

    public static Payments payments() {
        Payments current = payments;
        if (current == null) {
            initialize(PaymentsClient.class);
            current = payments;
        }
        return current;
    }

    private static void initialize(Class<?> type) {
        try {
            Class.forName(type.getName(), true, type.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }
}

package com.prestouniverse.pay.sample.demo.model.checkout;

import com.prestouniverse.pay.payments.PaymentMethod;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PaymentMethodCatalog {

    private PaymentMethodCatalog() {
    }

    public static List<PaymentMethodChoice> selectableMethods() {
        return Collections.unmodifiableList(Arrays.asList(
                new PaymentMethodChoice(PaymentMethod.PM_PG_CARD, "Credit / debit card", "Cards"),
                new PaymentMethodChoice(PaymentMethod.TOUCH_N_GO_E_WALLET, "Touch 'n Go eWallet", "E-wallets"),
                new PaymentMethodChoice(PaymentMethod.GRAB_PAY, "GrabPay", "E-wallets"),
                new PaymentMethodChoice(PaymentMethod.MAYBANK, "Maybank FPX", "Online banking"),
                new PaymentMethodChoice(PaymentMethod.CIMB, "CIMB Clicks", "Online banking"),
                new PaymentMethodChoice(PaymentMethod.PUBLIC_BANK, "Public Bank", "Online banking")));
    }
}

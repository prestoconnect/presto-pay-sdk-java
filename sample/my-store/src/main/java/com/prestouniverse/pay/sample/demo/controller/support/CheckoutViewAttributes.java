package com.prestouniverse.pay.sample.demo.controller.support;

import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.sample.demo.model.CheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import org.springframework.ui.Model;

import java.math.BigDecimal;

public final class CheckoutViewAttributes {

    private CheckoutViewAttributes() {
    }

    /**
     * Populates the single checkout page's model: the one checkout form (its own
     * {@code showPaymentMethods} field drives the toggle). The payment method list and its category
     * pills are static data handled entirely client-side in the page's own JavaScript.
     */
    public static void populateCheckoutPage(Model model, PaymentActivityStore activityStore, CheckoutForm form) {
        model.addAttribute("checkout", form);
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
    }

    public static CheckoutForm defaultForm() {
        CheckoutForm form = new CheckoutForm();
        form.setPageTitle("MyStore");
        form.setDisplayDesc("Checkout demo");
        form.setAmountInRinggit(new BigDecimal("10.00"));
        form.setShowPaymentMethods(false);
        form.setSelectedPaymentMethod(PaymentMethod.PmPgCard);
        return form;
    }
}

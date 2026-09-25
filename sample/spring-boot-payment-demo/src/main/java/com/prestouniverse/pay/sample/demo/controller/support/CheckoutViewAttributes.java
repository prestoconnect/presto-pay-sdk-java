package com.prestouniverse.pay.sample.demo.controller.support;

import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.sample.demo.model.checkout.CheckoutForm;
import com.prestouniverse.pay.sample.demo.model.checkout.PaymentMethodCatalog;
import com.prestouniverse.pay.sample.demo.model.checkout.PaymentMethodChoice;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

public final class CheckoutViewAttributes {

    private CheckoutViewAttributes() {
    }

    /**
     * Populates the single checkout page's model: the one checkout form (its own
     * {@code showPaymentMethods} field drives the toggle), and the payment method catalog and its
     * categories (the "All / Cards / ..." filter pills are applied client-side in JS from these).
     */
    public static void populateCheckoutPage(Model model, PaymentActivityStore activityStore, CheckoutForm form) {
        model.addAttribute("checkout", form);
        model.addAttribute("paymentMethods", PaymentMethodCatalog.selectableMethods());
        model.addAttribute("paymentMethodCategories", paymentMethodCategories());
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
    }

    private static Set<String> paymentMethodCategories() {
        Set<String> categories = new LinkedHashSet<String>();
        for (PaymentMethodChoice method : PaymentMethodCatalog.selectableMethods()) {
            categories.add(method.getCategory());
        }
        return categories;
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

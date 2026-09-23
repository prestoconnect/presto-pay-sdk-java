package com.prestouniverse.pay.sample.demo.controller.support;

import com.prestouniverse.pay.payments.PaymentMethod;
import com.prestouniverse.pay.sample.demo.model.checkout.HostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.model.checkout.PaymentMethodCatalog;
import com.prestouniverse.pay.sample.demo.model.checkout.SelfHostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import org.springframework.ui.Model;

import java.math.BigDecimal;

public final class CheckoutViewAttributes {

    private CheckoutViewAttributes() {
    }

    public static void populateHostedPage(Model model, PaymentActivityStore activityStore, HostedCheckoutForm form) {
        model.addAttribute("checkout", form);
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
    }

    public static void populateSelfHostedPage(Model model, PaymentActivityStore activityStore,
            SelfHostedCheckoutForm form) {
        model.addAttribute("checkout", form);
        model.addAttribute("paymentMethods", PaymentMethodCatalog.selectableMethods());
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
    }

    public static HostedCheckoutForm defaultHostedForm() {
        HostedCheckoutForm form = new HostedCheckoutForm();
        form.setDisplayDesc("Hosted checkout demo");
        form.setAmountInRinggit(new BigDecimal("25.00"));
        return form;
    }

    public static SelfHostedCheckoutForm defaultSelfHostedForm() {
        SelfHostedCheckoutForm form = new SelfHostedCheckoutForm();
        form.setPageTitle("Acme Store");
        form.setDisplayDesc("Self-hosted checkout — SDK sample");
        form.setAmountInRinggit(new BigDecimal("10.00"));
        form.setSelectedPaymentMethod(PaymentMethod.PM_PG_CARD);
        return form;
    }
}

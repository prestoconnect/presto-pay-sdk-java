package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.controller.support.PaymentInitRedirect;
import com.prestouniverse.pay.sample.demo.model.checkout.SelfHostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.service.WebPayCheckoutService;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/self-hosted")
public class SelfHostedCheckoutController {

    private final WebPayCheckoutService checkoutService;
    private final PaymentActivityStore activityStore;

    public SelfHostedCheckoutController(WebPayCheckoutService checkoutService, PaymentActivityStore activityStore) {
        this.checkoutService = checkoutService;
        this.activityStore = activityStore;
    }

    @GetMapping
    public String showForm(Model model) {
        CheckoutViewAttributes.populateSelfHostedPage(model, activityStore,
                CheckoutViewAttributes.defaultSelfHostedForm());
        return "self-hosted";
    }

    @PostMapping
    public String submitPayment(@Valid @ModelAttribute("checkout") SelfHostedCheckoutForm form,
            BindingResult validation, Model model) {
        if (!StringUtils.hasText(form.getSelectedPaymentMethod())) {
            validation.rejectValue("selectedPaymentMethod", "required", "Select a payment method");
        }
        if (validation.hasErrors()) {
            CheckoutViewAttributes.populateSelfHostedPage(model, activityStore, form);
            return "self-hosted";
        }
        return PaymentInitRedirect.viewAfterInit(checkoutService.initiateSelfHostedPayment(form));
    }
}

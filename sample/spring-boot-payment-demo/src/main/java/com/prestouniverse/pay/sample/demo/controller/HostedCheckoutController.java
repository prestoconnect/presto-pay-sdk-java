package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.controller.support.PaymentInitRedirect;
import com.prestouniverse.pay.sample.demo.model.checkout.HostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.service.WebPayCheckoutService;
import javax.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/hosted")
public class HostedCheckoutController {

    private final WebPayCheckoutService checkoutService;
    private final PaymentActivityStore activityStore;

    public HostedCheckoutController(WebPayCheckoutService checkoutService, PaymentActivityStore activityStore) {
        this.checkoutService = checkoutService;
        this.activityStore = activityStore;
    }

    @GetMapping
    public String showForm(Model model) {
        CheckoutViewAttributes.populateHostedPage(model, activityStore, CheckoutViewAttributes.defaultHostedForm());
        return "hosted";
    }

    @PostMapping
    public String submitPayment(@Valid @ModelAttribute("checkout") HostedCheckoutForm form, BindingResult validation,
            Model model) {
        if (validation.hasErrors()) {
            CheckoutViewAttributes.populateHostedPage(model, activityStore, form);
            return "hosted";
        }
        return PaymentInitRedirect.viewAfterInit(checkoutService.checkout(form));
    }
}

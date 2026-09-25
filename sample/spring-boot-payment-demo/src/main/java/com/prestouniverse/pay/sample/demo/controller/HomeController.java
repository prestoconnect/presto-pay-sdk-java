package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.controller.support.PaymentInitRedirect;
import com.prestouniverse.pay.sample.demo.model.checkout.CheckoutForm;
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

@Controller
public class HomeController {

    private final WebPayCheckoutService checkoutService;
    private final PaymentActivityStore activityStore;

    public HomeController(WebPayCheckoutService checkoutService, PaymentActivityStore activityStore) {
        this.checkoutService = checkoutService;
        this.activityStore = activityStore;
    }

    @GetMapping("/")
    public String home(Model model) {
        CheckoutViewAttributes.populateCheckoutPage(model, activityStore, CheckoutViewAttributes.defaultForm());
        return "index";
    }

    @PostMapping("/checkout")
    public String submitPayment(@Valid @ModelAttribute("checkout") CheckoutForm form, BindingResult validation,
            Model model) {
        if (form.isShowPaymentMethods() && !StringUtils.hasText(form.getSelectedPaymentMethod())) {
            validation.rejectValue("selectedPaymentMethod", "required", "Select a payment method");
        }
        if (validation.hasErrors()) {
            CheckoutViewAttributes.populateCheckoutPage(model, activityStore, form);
            return "index";
        }
        return PaymentInitRedirect.viewAfterInit(checkoutService.checkout(form));
    }
}

package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.sample.demo.controller.support.CheckoutResponse;
import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.model.CheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.service.WebPayCheckoutService;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

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

    /**
     * JSON checkout API consumed by the page's own JavaScript. Returns {@code 200} with
     * {@link CheckoutResponse} ({@code paymentUrl} / {@code txnRefNum}) on success, {@code 400} with a
     * field-name to message map on validation failure. Gateway failures (signature, transport, API
     * errors) are handled by {@link com.prestouniverse.pay.sample.demo.controller.handler.CheckoutExceptionHandler}.
     */
    @PostMapping("/checkout")
    @ResponseBody
    public ResponseEntity<?> submitPayment(@Valid @RequestBody CheckoutForm form, BindingResult validation) {
        if (form.isShowPaymentMethods() && !StringUtils.hasText(form.getSelectedPaymentMethod())) {
            validation.rejectValue("selectedPaymentMethod", "required", "Select a payment method");
        }
        if (validation.hasErrors()) {
            return ResponseEntity.badRequest().body(fieldErrors(validation));
        }

        PaymentInitResponse response = checkoutService.checkout(form);
        return ResponseEntity.ok(new CheckoutResponse(response.paymentUrl(), response.txnRefNum()));
    }

    private static Map<String, String> fieldErrors(BindingResult validation) {
        Map<String, String> errors = new LinkedHashMap<String, String>();
        for (FieldError error : validation.getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return errors;
    }
}

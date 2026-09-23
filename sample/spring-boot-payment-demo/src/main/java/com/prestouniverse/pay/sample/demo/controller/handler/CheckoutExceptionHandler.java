package com.prestouniverse.pay.sample.demo.controller.handler;

import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayException;
import com.prestouniverse.pay.sample.demo.controller.HostedCheckoutController;
import com.prestouniverse.pay.sample.demo.controller.SelfHostedCheckoutController;
import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.model.checkout.HostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.model.checkout.SelfHostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = {HostedCheckoutController.class, SelfHostedCheckoutController.class})
public class CheckoutExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckoutExceptionHandler.class);

    private final PaymentActivityStore activityStore;

    public CheckoutExceptionHandler(PaymentActivityStore activityStore) {
        this.activityStore = activityStore;
    }

    @ExceptionHandler(PrestoPayException.class)
    public String handleCheckoutFailure(PrestoPayException exception, Model model, HttpServletRequest request) {
        if (exception instanceof PrestoPayApiException) {
            PrestoPayApiException apiError = (PrestoPayApiException) exception;
            log.warn("Checkout Presto API error path={} httpStatus={} errorCode={} systemError={} message={}",
                    request.getRequestURI(),
                    apiError.httpStatus(),
                    apiError.errorCode(),
                    apiError.isSystemError(),
                    apiError.errorMessage());
            model.addAttribute("errorCode", apiError.errorCode());
            model.addAttribute("errorMessage", apiError.errorMessage());
            model.addAttribute("systemError", apiError.isSystemError());
        } else {
            log.warn("Checkout failed path={}: {}", request.getRequestURI(), exception.getMessage());
        }
        model.addAttribute("message", exception.getMessage());
        model.addAttribute("paymentError", true);

        if (isSelfHostedRequest(request)) {
            SelfHostedCheckoutForm form = model.containsAttribute("checkout")
                    ? (SelfHostedCheckoutForm) model.getAttribute("checkout")
                    : CheckoutViewAttributes.defaultSelfHostedForm();
            CheckoutViewAttributes.populateSelfHostedPage(model, activityStore, form);
            return "self-hosted";
        }

        HostedCheckoutForm form = model.containsAttribute("checkout")
                ? (HostedCheckoutForm) model.getAttribute("checkout")
                : CheckoutViewAttributes.defaultHostedForm();
        CheckoutViewAttributes.populateHostedPage(model, activityStore, form);
        return "hosted";
    }

    private static boolean isSelfHostedRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        String selfHostedPrefix = request.getContextPath() + "/self-hosted";
        return path != null && path.startsWith(selfHostedPrefix);
    }
}

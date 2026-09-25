package com.prestouniverse.pay.sample.demo.controller.handler;

import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayException;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.sample.demo.controller.HomeController;
import com.prestouniverse.pay.sample.demo.controller.support.CheckoutViewAttributes;
import com.prestouniverse.pay.sample.demo.model.checkout.CheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice(assignableTypes = HomeController.class)
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
        } else if (exception instanceof PrestoPaySignatureException) {
            PrestoPaySignatureException signatureError = (PrestoPaySignatureException) exception;
            log.warn("Checkout signature verification failed path={} side={} message={} canonical={}",
                    request.getRequestURI(),
                    signatureError.side(),
                    signatureError.getMessage(),
                    signatureError.canonicalString());
            model.addAttribute("signatureError", true);
            model.addAttribute("signatureSide", signatureError.side().name());
        } else if (exception instanceof PrestoPayResponseException) {
            log.warn("Checkout received an unparseable Presto response path={} message={}; the payment may exist, "
                    + "reconcile with query by txnRefNum", request.getRequestURI(), exception.getMessage());
        } else {
            log.warn("Checkout failed path={}: {}", request.getRequestURI(), exception.getMessage());
        }
        model.addAttribute("message", exception.getMessage());
        model.addAttribute("paymentError", true);

        CheckoutForm form = model.containsAttribute("checkout")
                ? (CheckoutForm) model.getAttribute("checkout")
                : CheckoutViewAttributes.defaultForm();
        CheckoutViewAttributes.populateCheckoutPage(model, activityStore, form);
        return "index";
    }
}

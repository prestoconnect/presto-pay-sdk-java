package com.prestouniverse.pay.sample.demo.controller.handler;

import com.prestouniverse.pay.exception.PrestoPayApiException;
import com.prestouniverse.pay.exception.PrestoPayException;
import com.prestouniverse.pay.exception.PrestoPayResponseException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.sample.demo.controller.HomeController;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Converts gateway failures from {@code POST /checkout} into a JSON error body. */
@ControllerAdvice(assignableTypes = HomeController.class)
public class CheckoutExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(CheckoutExceptionHandler.class);

    @ExceptionHandler(PrestoPayException.class)
    public ResponseEntity<Map<String, Object>> handleCheckoutFailure(PrestoPayException exception,
            HttpServletRequest request) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("message", exception.getMessage());

        if (exception instanceof PrestoPayApiException) {
            PrestoPayApiException apiError = (PrestoPayApiException) exception;
            log.warn("Checkout Presto API error path={} httpStatus={} errorCode={} systemError={} message={}",
                    request.getRequestURI(),
                    apiError.httpStatus(),
                    apiError.errorCode(),
                    apiError.isSystemError(),
                    apiError.errorMessage());
            body.put("errorCode", apiError.errorCode());
            body.put("errorMessage", apiError.errorMessage());
        } else if (exception instanceof PrestoPaySignatureException) {
            PrestoPaySignatureException signatureError = (PrestoPaySignatureException) exception;
            log.warn("Checkout signature verification failed path={} side={} message={} canonical={}",
                    request.getRequestURI(),
                    signatureError.side(),
                    signatureError.getMessage(),
                    signatureError.canonicalString());
            body.put("signatureError", true);
        } else if (exception instanceof PrestoPayResponseException) {
            log.warn("Checkout received an unparseable Presto response path={} message={}; the payment may exist, "
                    + "reconcile with query by txnRefNum", request.getRequestURI(), exception.getMessage());
        } else {
            log.warn("Checkout failed path={}: {}", request.getRequestURI(), exception.getMessage());
        }

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}

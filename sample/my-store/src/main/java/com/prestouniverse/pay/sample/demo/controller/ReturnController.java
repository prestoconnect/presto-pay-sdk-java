package com.prestouniverse.pay.sample.demo.controller;

import com.prestouniverse.pay.exception.PrestoPayException;
import com.prestouniverse.pay.exception.PrestoPaySignatureException;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.service.WebPayCheckoutService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ReturnController {

    private static final Logger log = LoggerFactory.getLogger(ReturnController.class);

    private final WebPayCheckoutService checkoutService;
    private final PaymentActivityStore activityStore;

    public ReturnController(WebPayCheckoutService checkoutService, PaymentActivityStore activityStore) {
        this.checkoutService = checkoutService;
        this.activityStore = activityStore;
    }

    @GetMapping("/return")
    public String returnWithoutTxnRef(Model model) {
        log.debug("Return page rejected: missing txnRefNum path segment");
        model.addAttribute("missingTxnRefNum", true);
        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
        return "return";
    }

    @GetMapping("/return/{txnRefNum}")
    public String showReturnPage(@PathVariable String txnRefNum, Model model) {
        if (!StringUtils.hasText(txnRefNum)) {
            return returnWithoutTxnRef(model);
        }

        String merchantTxnRef = txnRefNum.trim();
        model.addAttribute("txnRefNum", merchantTxnRef);
        activityStore.findCheckoutByTxnRef(merchantTxnRef).ifPresent(record -> model.addAttribute("checkout", record));

        try {
            // Any status other than PaymentStatus.PendingAuthorise means Presto has finalised the
            // payment (Authorised, Failed, Cancelled, Expired, etc.). The payer's browser redirect
            // here and the /presto/notify webhook are triggered independently by Presto and can
            // arrive in either order, or at nearly the same time -- this page must not assume the
            // webhook has (or hasn't) already been processed.
            PaymentQueryResponse queryResult = checkoutService.query(merchantTxnRef);
            model.addAttribute("query", queryResult);
        } catch (PrestoPaySignatureException ex) {
            log.warn("Query signature verification failed txnRefNum={} side={} message={} canonical={}",
                    merchantTxnRef, ex.side(), ex.getMessage(), ex.canonicalString());
            model.addAttribute("querySignatureError", true);
            model.addAttribute("signatureSide", ex.side().name());
            model.addAttribute("queryError", ex.getMessage());
        } catch (PrestoPayException ex) {
            log.warn("Query failed for txnRefNum={}: {}", merchantTxnRef, ex.getMessage());
            model.addAttribute("queryError", ex.getMessage());
        }

        model.addAttribute("recentWebhooks", activityStore.recentWebhooks());
        return "return";
    }
}

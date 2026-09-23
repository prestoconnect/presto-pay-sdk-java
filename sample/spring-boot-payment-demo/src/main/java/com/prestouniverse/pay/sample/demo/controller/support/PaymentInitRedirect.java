package com.prestouniverse.pay.sample.demo.controller.support;

import com.prestouniverse.pay.payments.PaymentInitResponse;
import org.springframework.util.StringUtils;

public final class PaymentInitRedirect {

    private PaymentInitRedirect() {
    }

    public static String viewAfterInit(PaymentInitResponse initResponse) {
        String paymentUrl = initResponse.paymentUrl();
        if (!StringUtils.hasText(paymentUrl)) {
            return "redirect:/return/" + initResponse.txnRefNum();
        }
        return "redirect:" + paymentUrl;
    }
}

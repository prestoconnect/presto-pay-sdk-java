package com.prestouniverse.pay.sample.demo.service;

import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.payments.PaymentInitRequest;
import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentQueryRequest;
import com.prestouniverse.pay.payments.PaymentQueryResponse;
import com.prestouniverse.pay.payments.TxnType;
import com.prestouniverse.pay.sample.demo.config.AppProperties;
import com.prestouniverse.pay.sample.demo.model.checkout.HostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.model.checkout.SelfHostedCheckoutForm;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore.CheckoutRecord;
import com.prestouniverse.pay.sample.demo.repository.PaymentActivityStore.SelfHostedCheckoutSnapshot;
import com.prestouniverse.pay.sample.demo.service.support.CheckoutAmounts;
import com.prestouniverse.pay.sample.demo.service.support.DemoTxnReferenceGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class WebPayCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(WebPayCheckoutService.class);

    private final PrestoPayClient prestoPayClient;
    private final AppProperties appProperties;
    private final PaymentActivityStore activityStore;

    public WebPayCheckoutService(PrestoPayClient prestoPayClient, AppProperties appProperties,
            PaymentActivityStore activityStore) {
        this.prestoPayClient = prestoPayClient;
        this.appProperties = appProperties;
        this.activityStore = activityStore;
    }

    public PaymentInitResponse checkout(HostedCheckoutForm form) {
        String txnRefNum = DemoTxnReferenceGenerator.next();
        int amountMinorUnits = CheckoutAmounts.toMinorUnits(form.getAmountInRinggit());
        String displayDesc = form.getDisplayDesc().trim();

        log.info("Initiating hosted WebPay txnRefNum={} amountMinorUnits={} currency={} displayDesc={}",
                txnRefNum, amountMinorUnits, appProperties.getDefaultCurrency(), displayDesc);

        CheckoutRecord pendingRecord = CheckoutRecord.forHostedInit(txnRefNum, displayDesc, amountMinorUnits,
                appProperties.getDefaultCurrency());

        return doInit(buildWebPayInitRequest(displayDesc, amountMinorUnits, txnRefNum), pendingRecord);
    }

    public PaymentInitResponse checkout(SelfHostedCheckoutForm form) {
        String txnRefNum = DemoTxnReferenceGenerator.next();
        int amountMinorUnits = CheckoutAmounts.toMinorUnits(form.getAmountInRinggit());
        String displayDesc = form.getDisplayDesc().trim();
        String selectedMethod = form.getSelectedPaymentMethod().trim();

        log.info("Initiating self-hosted WebPay txnRefNum={} amountMinorUnits={} currency={} "
                        + "allowedPaymentMethods={} displayDesc={}",
                txnRefNum, amountMinorUnits, appProperties.getDefaultCurrency(), selectedMethod, displayDesc);

        PaymentInitRequest.Builder initRequest = buildWebPayInitRequest(displayDesc, amountMinorUnits, txnRefNum)
                .allowedPaymentMethods(selectedMethod);

        applyOptionalReceiptFields(initRequest, form);

        CheckoutRecord pendingRecord = CheckoutRecord.forSelfHostedInit(txnRefNum,
                new SelfHostedCheckoutSnapshot(form.getPageTitle(), displayDesc, selectedMethod,
                        form.getReceiptName(), form.getReceiptEmail()),
                amountMinorUnits, appProperties.getDefaultCurrency());

        return doInit(initRequest, pendingRecord);
    }

    public PaymentQueryResponse query(String txnRefNum) {
        log.info("Querying payment txnRefNum={}", txnRefNum);
        PaymentQueryResponse response = prestoPayClient.payments().query(
                PaymentQueryRequest.builder().txnRefNum(txnRefNum).build());
        log.info("Query completed txnRefNum={} paymentRefNum={} paymentStatus={} amount={} {}",
                response.txnRefNum(),
                response.paymentRefNum(),
                response.paymentStatus(),
                response.amount(),
                response.currencyCode());
        return response;
    }

    private PaymentInitRequest.Builder buildWebPayInitRequest(String displayDesc, int amountMinorUnits,
            String txnRefNum) {
        return PaymentInitRequest.builder()
                .txnType(TxnType.WEB_PAY)
                .txnRefNum(txnRefNum)
                .displayDesc(displayDesc)
                .amount(amountMinorUnits)
                .currencyCode(appProperties.getDefaultCurrency())
                .notifyUrl(appProperties.notifyUrl())
                .redirectUrl(appProperties.returnUrlForTransaction(txnRefNum));
    }

    private static void applyOptionalReceiptFields(PaymentInitRequest.Builder initRequest,
            SelfHostedCheckoutForm form) {
        if (StringUtils.hasText(form.getReceiptName())) {
            initRequest.receiptName(form.getReceiptName().trim());
        }
        if (StringUtils.hasText(form.getReceiptEmail())) {
            initRequest.receiptEmail(form.getReceiptEmail().trim());
        }
    }

    private PaymentInitResponse doInit(PaymentInitRequest.Builder initRequestBuilder, CheckoutRecord pending) {
        PaymentInitResponse response = prestoPayClient.payments().init(initRequestBuilder.build());
        log.info("WebPay init succeeded txnRefNum={} paymentRefNum={} paymentStatus={} paymentUrlPresent={}",
                response.txnRefNum(),
                response.paymentRefNum(),
                response.paymentStatus(),
                StringUtils.hasText(response.paymentUrl()));

        activityStore.saveCheckout(pending.afterSuccessfulInit(response));
        return response;
    }
}

package com.prestouniverse.pay.sample.demo.repository;

import com.prestouniverse.pay.payments.PaymentInitResponse;
import com.prestouniverse.pay.payments.PaymentStatus;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class PaymentActivityStore {

    private static final int MAX_WEBHOOK_HISTORY = 50;

    private static final Set<String> PAID_AND_STILL_OPEN = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(PaymentStatus.Authorised, PaymentStatus.PendingReverse, PaymentStatus.PendingRefund,
                    PaymentStatus.PartialRefunded)));
    private static final Set<String> AFTER_PAYMENT = Collections.unmodifiableSet(new HashSet<String>(
            Arrays.asList(PaymentStatus.Authorised, PaymentStatus.PendingReverse, PaymentStatus.PendingRefund,
                    PaymentStatus.PartialRefunded, PaymentStatus.Reversed, PaymentStatus.Refunded)));

    public enum StatusChange {
        NONE,
        UPDATED,
        PAID
    }

    private final Map<String, CheckoutRecord> checkoutByTxnRef = new ConcurrentHashMap<String, CheckoutRecord>();
    private final Map<String, String> orderStatusByTxnRef = new HashMap<String, String>();
    private final List<WebhookRecord> webhookHistory = Collections.synchronizedList(new ArrayList<WebhookRecord>());

    public void saveCheckout(CheckoutRecord record) {
        checkoutByTxnRef.put(record.getTxnRefNum(), record);
        if (record.getPaymentStatus() != null) {
            synchronized (orderStatusByTxnRef) {
                if (!orderStatusByTxnRef.containsKey(record.getTxnRefNum())) {
                    orderStatusByTxnRef.put(record.getTxnRefNum(), record.getPaymentStatus());
                }
            }
        }
    }

    /**
     * A real store makes this one conditional UPDATE on the orders table, so that only one of the return page and
     * the webhook finalises the order.
     */
    public StatusChange applyPaymentStatus(String txnRefNum, String next) {
        synchronized (orderStatusByTxnRef) {
            String current = orderStatusByTxnRef.get(txnRefNum);
            if (!canChangeStatus(current, next)) {
                return StatusChange.NONE;
            }
            orderStatusByTxnRef.put(txnRefNum, next);
            boolean paidNow = PaymentStatus.Authorised.equals(next)
                    && (current == null || PaymentStatus.PendingAuthorise.equals(current));
            return paidNow ? StatusChange.PAID : StatusChange.UPDATED;
        }
    }

    static boolean canChangeStatus(String current, String next) {
        if (next.equals(current)) {
            return false;
        }
        if (current == null || PaymentStatus.PendingAuthorise.equals(current)) {
            return true;
        }
        if (PAID_AND_STILL_OPEN.contains(current)) {
            return AFTER_PAYMENT.contains(next);
        }
        return false;
    }

    public void appendWebhook(WebhookRecord record) {
        webhookHistory.add(0, record);
        if (webhookHistory.size() > MAX_WEBHOOK_HISTORY) {
            webhookHistory.remove(webhookHistory.size() - 1);
        }
    }

    public Optional<CheckoutRecord> findCheckoutByTxnRef(String txnRefNum) {
        return Optional.ofNullable(checkoutByTxnRef.get(txnRefNum));
    }

    public List<WebhookRecord> recentWebhooks() {
        synchronized (webhookHistory) {
            return Collections.unmodifiableList(new ArrayList<WebhookRecord>(webhookHistory));
        }
    }

    public static final class CheckoutRecord {

        private final String txnRefNum;
        private final String displayDesc;
        private final String pageTitle;
        private final int amountMinorUnits;
        private final String currencyCode;
        private final String selectedPaymentMethod;
        private final String receiptName;
        private final String receiptEmail;
        private final String paymentRefNum;
        private final String paymentStatus;
        private final Instant initiatedAt;

        private CheckoutRecord(String txnRefNum, String displayDesc, String pageTitle,
                int amountMinorUnits, String currencyCode, String selectedPaymentMethod, String receiptName,
                String receiptEmail, String paymentRefNum, String paymentStatus, Instant initiatedAt) {
            this.txnRefNum = txnRefNum;
            this.displayDesc = displayDesc;
            this.pageTitle = pageTitle;
            this.amountMinorUnits = amountMinorUnits;
            this.currencyCode = currencyCode;
            this.selectedPaymentMethod = selectedPaymentMethod;
            this.receiptName = receiptName;
            this.receiptEmail = receiptEmail;
            this.paymentRefNum = paymentRefNum;
            this.paymentStatus = paymentStatus;
            this.initiatedAt = initiatedAt;
        }

        public static CheckoutRecord forHostedInit(String txnRefNum, String displayDesc, int amountMinorUnits,
                String currencyCode) {
            return new CheckoutRecord(txnRefNum, displayDesc, null, amountMinorUnits,
                    currencyCode, null, null, null, null, null, Instant.now());
        }

        public static CheckoutRecord forSelfHostedInit(String txnRefNum, SelfHostedCheckoutSnapshot form,
                int amountMinorUnits, String currencyCode) {
            return new CheckoutRecord(txnRefNum, form.displayDesc, form.pageTitle,
                    amountMinorUnits, currencyCode, form.selectedPaymentMethod, form.receiptName, form.receiptEmail,
                    null, null, Instant.now());
        }

        public CheckoutRecord afterSuccessfulInit(PaymentInitResponse initResponse) {
            return new CheckoutRecord(initResponse.txnRefNum(), displayDesc, pageTitle, amountMinorUnits,
                    currencyCode, selectedPaymentMethod, receiptName, receiptEmail, initResponse.paymentRefNum(),
                    initResponse.paymentStatus(), initiatedAt);
        }

        public String getTxnRefNum() {
            return txnRefNum;
        }

        public String getDisplayDesc() {
            return displayDesc;
        }

        public String getPageTitle() {
            return pageTitle;
        }

        public int getAmountMinorUnits() {
            return amountMinorUnits;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public String getSelectedPaymentMethod() {
            return selectedPaymentMethod;
        }

        public String getReceiptName() {
            return receiptName;
        }

        public String getReceiptEmail() {
            return receiptEmail;
        }

        public String getPaymentRefNum() {
            return paymentRefNum;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public Instant getInitiatedAt() {
            return initiatedAt;
        }
    }

    public static final class SelfHostedCheckoutSnapshot {

        public final String pageTitle;
        public final String displayDesc;
        public final String selectedPaymentMethod;
        public final String receiptName;
        public final String receiptEmail;

        public SelfHostedCheckoutSnapshot(String pageTitle, String displayDesc, String selectedPaymentMethod,
                String receiptName, String receiptEmail) {
            this.pageTitle = pageTitle;
            this.displayDesc = displayDesc;
            this.selectedPaymentMethod = selectedPaymentMethod;
            this.receiptName = receiptName;
            this.receiptEmail = receiptEmail;
        }
    }

    public static final class WebhookRecord {

        private final String txnRefNum;
        private final String eventCode;
        private final String paymentStatus;
        private final boolean success;
        private final int amountMinorUnits;
        private final String currencyCode;
        private final Instant receivedAt;

        public WebhookRecord(String txnRefNum, String eventCode, String paymentStatus, boolean success,
                int amountMinorUnits, String currencyCode, Instant receivedAt) {
            this.txnRefNum = txnRefNum;
            this.eventCode = eventCode;
            this.paymentStatus = paymentStatus;
            this.success = success;
            this.amountMinorUnits = amountMinorUnits;
            this.currencyCode = currencyCode;
            this.receivedAt = receivedAt;
        }

        public String getTxnRefNum() {
            return txnRefNum;
        }

        public String getEventCode() {
            return eventCode;
        }

        public String getPaymentStatus() {
            return paymentStatus;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getAmountMinorUnits() {
            return amountMinorUnits;
        }

        public String getCurrencyCode() {
            return currencyCode;
        }

        public Instant getReceivedAt() {
            return receivedAt;
        }
    }
}

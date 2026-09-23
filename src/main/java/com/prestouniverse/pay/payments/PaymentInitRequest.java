package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.Timestamps;
import com.prestouniverse.pay.internal.json.JsonArray;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class PaymentInitRequest {

    private final String merchantRefNum;
    private final String qrValue;
    private final String payerRefNum;
    private final String deviceRefNum;
    private final String deviceIp;
    private final String txnType;
    private final String txnRefNum;
    private final String displayDesc;
    private final List<LineItem> items;
    private final String transactionalData;
    private final Integer amount;
    private final String currencyCode;
    private final String notifyUrl;
    private final String redirectUrl;
    private final Instant sessionValidity;
    private final String additionalData;
    private final String mode;
    private final String modeData;
    private final List<String> allowedPaymentMethods;
    private final String bindData;
    private final String themeRefNum;
    private final String receiptEmail;
    private final String receiptName;

    private PaymentInitRequest(Builder builder) {
        this.merchantRefNum = builder.merchantRefNum;
        this.qrValue = builder.qrValue;
        this.payerRefNum = builder.payerRefNum;
        this.deviceRefNum = builder.deviceRefNum;
        this.deviceIp = builder.deviceIp;
        this.txnType = builder.txnType;
        this.txnRefNum = builder.txnRefNum;
        this.displayDesc = builder.displayDesc;
        this.items = Collections.unmodifiableList(new ArrayList<>(builder.items));
        this.transactionalData = builder.transactionalData;
        this.amount = builder.amount;
        this.currencyCode = builder.currencyCode;
        this.notifyUrl = builder.notifyUrl;
        this.redirectUrl = builder.redirectUrl;
        this.sessionValidity = builder.sessionValidity;
        this.additionalData = builder.additionalData;
        this.mode = builder.mode;
        this.modeData = builder.modeData;
        this.allowedPaymentMethods = Collections.unmodifiableList(new ArrayList<>(builder.allowedPaymentMethods));
        this.bindData = builder.bindData;
        this.themeRefNum = builder.themeRefNum;
        this.receiptEmail = builder.receiptEmail;
        this.receiptName = builder.receiptName;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String merchantRefNum() {
        return merchantRefNum;
    }

    JsonObject toJson() {
        JsonObject node = JsonCodec.newObject();
        JsonCodec.putIfPresent(node, "qrValue", qrValue);
        JsonCodec.putIfPresent(node, "payerRefNum", payerRefNum);
        JsonCodec.putIfPresent(node, "deviceRefNum", deviceRefNum);
        JsonCodec.putIfPresent(node, "deviceIp", deviceIp);
        node.put("txnType", txnType);
        node.put("txnRefNum", txnRefNum);
        node.put("displayDesc", displayDesc);
        if (!items.isEmpty()) {
            node.put("itemList", itemsToJson());
        }
        JsonCodec.putIfPresent(node, "transactionalData", transactionalData);
        JsonCodec.putIfPresent(node, "amount", amount);
        JsonCodec.putIfPresent(node, "currencyCode", currencyCode);
        JsonCodec.putIfPresent(node, "notifyUrl", notifyUrl);
        JsonCodec.putIfPresent(node, "redirectUrl", redirectUrl);
        if (sessionValidity != null) {
            node.put("sessionValidity", Timestamps.format(sessionValidity));
        }
        JsonCodec.putIfPresent(node, "additionalData", additionalData);
        JsonCodec.putIfPresent(node, "mode", mode);
        JsonCodec.putIfPresent(node, "modeData", modeData);
        if (!allowedPaymentMethods.isEmpty()) {
            JsonCodec.putStringArray(node, "allowedPaymentMethods", allowedPaymentMethods);
        }
        JsonCodec.putIfPresent(node, "bindData", bindData);
        JsonCodec.putIfPresent(node, "themeRefNum", themeRefNum);
        JsonCodec.putIfPresent(node, "receiptEmail", receiptEmail);
        JsonCodec.putIfPresent(node, "receiptName", receiptName);
        return node;
    }

    private String itemsToJson() {
        JsonArray array = JsonCodec.newArray();
        for (LineItem item : items) {
            array.add(item.toJson());
        }
        return array.toString();
    }

    public static final class Builder {

        private String merchantRefNum;
        private String qrValue;
        private String payerRefNum;
        private String deviceRefNum;
        private String deviceIp;
        private String txnType;
        private String txnRefNum;
        private String displayDesc;
        private List<LineItem> items = new ArrayList<>();
        private String transactionalData;
        private Integer amount;
        private String currencyCode;
        private String notifyUrl;
        private String redirectUrl;
        private Instant sessionValidity;
        private String additionalData;
        private String mode;
        private String modeData;
        private List<String> allowedPaymentMethods = new ArrayList<>();
        private String bindData;
        private String themeRefNum;
        private String receiptEmail;
        private String receiptName;

        private Builder() {
        }

        public Builder merchantRefNum(String merchantRefNum) {
            this.merchantRefNum = merchantRefNum;
            return this;
        }

        public Builder qrValue(String qrValue) {
            this.qrValue = qrValue;
            return this;
        }

        public Builder payerRefNum(String payerRefNum) {
            this.payerRefNum = payerRefNum;
            return this;
        }

        public Builder deviceRefNum(String deviceRefNum) {
            this.deviceRefNum = deviceRefNum;
            return this;
        }

        public Builder deviceIp(String deviceIp) {
            this.deviceIp = deviceIp;
            return this;
        }

        public Builder txnType(String txnType) {
            this.txnType = txnType;
            return this;
        }

        public Builder txnRefNum(String txnRefNum) {
            this.txnRefNum = txnRefNum;
            return this;
        }

        public Builder displayDesc(String displayDesc) {
            this.displayDesc = displayDesc;
            return this;
        }

        public Builder items(LineItem... items) {
            return items(items == null ? null : Arrays.asList(items));
        }

        public Builder items(List<LineItem> items) {
            this.items = Validation.copyOf(items);
            return this;
        }

        public Builder transactionalData(String transactionalData) {
            this.transactionalData = transactionalData;
            return this;
        }

        public Builder amount(int amount) {
            this.amount = amount;
            return this;
        }

        public Builder currencyCode(String currencyCode) {
            this.currencyCode = currencyCode;
            return this;
        }

        public Builder notifyUrl(String notifyUrl) {
            this.notifyUrl = notifyUrl;
            return this;
        }

        public Builder redirectUrl(String redirectUrl) {
            this.redirectUrl = redirectUrl;
            return this;
        }

        public Builder sessionValidity(Instant sessionValidity) {
            this.sessionValidity = sessionValidity;
            return this;
        }

        public Builder additionalData(String additionalData) {
            this.additionalData = additionalData;
            return this;
        }

        public Builder mode(String mode) {
            this.mode = mode;
            return this;
        }

        public Builder modeData(String modeData) {
            this.modeData = modeData;
            return this;
        }

        public Builder allowedPaymentMethods(String... methods) {
            return allowedPaymentMethods(methods == null ? null : Arrays.asList(methods));
        }

        public Builder allowedPaymentMethods(List<String> methods) {
            this.allowedPaymentMethods = Validation.copyOf(methods);
            return this;
        }

        public Builder bindData(String bindData) {
            this.bindData = bindData;
            return this;
        }

        public Builder themeRefNum(String themeRefNum) {
            this.themeRefNum = themeRefNum;
            return this;
        }

        public Builder receiptEmail(String receiptEmail) {
            this.receiptEmail = receiptEmail;
            return this;
        }

        public Builder receiptName(String receiptName) {
            this.receiptName = receiptName;
            return this;
        }

        public PaymentInitRequest build() {
            Validation.requireNonBlank("merchantRefNum", merchantRefNum);
            Validation.requireNonNull("txnType", txnType);
            Validation.requireNonNull("txnRefNum", txnRefNum);
            Validation.requireNonNull("displayDesc", displayDesc);
            Validation.requireMaxLength("txnRefNum", txnRefNum, 50);
            Validation.requireMaxLength("displayDesc", displayDesc, 255);
            Validation.requireMaxLength("deviceRefNum", deviceRefNum, 50);
            Validation.requireMaxLength("deviceIp", deviceIp, 50);
            Validation.requireMaxLength("notifyUrl", notifyUrl, 255);
            Validation.requireMaxLength("redirectUrl", redirectUrl, 255);
            Validation.requireMaxLength("additionalData", additionalData, 255);
            Validation.requireMaxLength("mode", mode, 50);
            Validation.requireMaxLength("modeData", modeData, 1000);
            Validation.requireMaxLength("receiptEmail", receiptEmail, 320);
            Validation.requireMaxLength("receiptName", receiptName, 200);
            Validation.requirePositive("amount", amount);
            Validation.requireNoNullElements("items", items);
            Validation.requireNoNullElements("allowedPaymentMethods", allowedPaymentMethods);

            if (qrValue != null && payerRefNum != null) {
                throw new PrestoPayConfigException("qrValue",
                        "qrValue and payerRefNum cannot both be set");
            }
            if (amount != null && currencyCode == null) {
                throw new PrestoPayConfigException("currencyCode",
                        "currencyCode is required when amount is set");
            }
            if (TxnType.WEB_PAY.equals(txnType) && redirectUrl == null) {
                throw new PrestoPayConfigException("redirectUrl",
                        "redirectUrl is required when txnType is WebPay");
            }

            return new PaymentInitRequest(this);
        }
    }
}

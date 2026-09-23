package com.prestouniverse.pay.sample.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    /**
     * Public HTTPS (or HTTP) origin of this app, used to build {@link #notifyUrl()} and {@link #redirectUrl()}.
     * Must be reachable from the internet so Presto can POST webhooks to {@code notifyUrl}; {@code localhost}
     * only works if you tunnel (for example ngrok). No trailing slash.
     */
    private String publicBaseUrl = "http://localhost:8080";

    private String defaultCurrency = "MYR";

    public String getPublicBaseUrl() {
        return publicBaseUrl;
    }

    public void setPublicBaseUrl(String publicBaseUrl) {
        this.publicBaseUrl = publicBaseUrl;
    }

    public String getDefaultCurrency() {
        return defaultCurrency;
    }

    public void setDefaultCurrency(String defaultCurrency) {
        this.defaultCurrency = defaultCurrency;
    }

    public String notifyUrl() {
        return publicBaseUrl + "/presto/notify";
    }

    /** Payer return path without query string (display only). */
    public String redirectUrl() {
        return publicBaseUrl + "/return";
    }

    /**
     * {@code redirectUrl} sent on WebPay init: {@code {base}/return/{txnRefNum}}.
     */
    public String returnUrlForTransaction(String txnRefNum) {
        if (txnRefNum == null || txnRefNum.trim().isEmpty()) {
            throw new IllegalArgumentException("txnRefNum is required to build the return URL");
        }
        return redirectUrl() + "/" + txnRefNum.trim();
    }
}

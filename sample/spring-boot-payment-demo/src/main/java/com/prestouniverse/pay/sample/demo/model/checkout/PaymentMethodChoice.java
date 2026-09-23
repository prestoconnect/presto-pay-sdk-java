package com.prestouniverse.pay.sample.demo.model.checkout;

public final class PaymentMethodChoice {

    private final String gatewayCode;
    private final String displayName;
    private final String category;

    public PaymentMethodChoice(String gatewayCode, String displayName, String category) {
        this.gatewayCode = gatewayCode;
        this.displayName = displayName;
        this.category = category;
    }

    public String getGatewayCode() {
        return gatewayCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCategory() {
        return category;
    }
}

package com.prestouniverse.pay.sample.demo.model.checkout;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.math.BigDecimal;

public class SelfHostedCheckoutForm {

    @NotBlank
    @Size(max = 80)
    private String pageTitle;

    @NotBlank
    @Size(max = 200)
    private String displayDesc;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amountInRinggit;

    /** Gateway wire value sent as {@code allowedPaymentMethods} on init. */
    @NotBlank
    private String selectedPaymentMethod;

    @Size(max = 200)
    private String receiptName;

    @Size(max = 320)
    private String receiptEmail;

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getDisplayDesc() {
        return displayDesc;
    }

    public void setDisplayDesc(String displayDesc) {
        this.displayDesc = displayDesc;
    }

    public BigDecimal getAmountInRinggit() {
        return amountInRinggit;
    }

    public void setAmountInRinggit(BigDecimal amountInRinggit) {
        this.amountInRinggit = amountInRinggit;
    }

    public String getSelectedPaymentMethod() {
        return selectedPaymentMethod;
    }

    public void setSelectedPaymentMethod(String selectedPaymentMethod) {
        this.selectedPaymentMethod = selectedPaymentMethod;
    }

    public String getReceiptName() {
        return receiptName;
    }

    public void setReceiptName(String receiptName) {
        this.receiptName = receiptName;
    }

    public String getReceiptEmail() {
        return receiptEmail;
    }

    public void setReceiptEmail(String receiptEmail) {
        this.receiptEmail = receiptEmail;
    }
}

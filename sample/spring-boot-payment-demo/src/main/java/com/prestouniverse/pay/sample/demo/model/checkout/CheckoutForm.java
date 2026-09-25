package com.prestouniverse.pay.sample.demo.model.checkout;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * The single checkout form backing the one {@code /checkout} submit endpoint. {@code showPaymentMethods}
 * mirrors the page's toggle: off sends no {@code allowedPaymentMethods} (the payer chooses on Presto's
 * hosted page); on requires {@code selectedPaymentMethod} and sends it as {@code allowedPaymentMethods}.
 */
public class CheckoutForm {

    @NotBlank
    @Size(max = 200)
    private String displayDesc;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amountInRinggit;

    private boolean showPaymentMethods;

    @Size(max = 80)
    private String pageTitle;

    /** Gateway wire value sent as {@code allowedPaymentMethods}; required only when {@code showPaymentMethods}. */
    private String selectedPaymentMethod;

    @Size(max = 200)
    private String receiptName;

    @Size(max = 320)
    private String receiptEmail;

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

    public boolean isShowPaymentMethods() {
        return showPaymentMethods;
    }

    public void setShowPaymentMethods(boolean showPaymentMethods) {
        this.showPaymentMethods = showPaymentMethods;
    }

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
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

package com.prestouniverse.pay.sample.demo.model.checkout;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import java.math.BigDecimal;

public class HostedCheckoutForm {

    @NotBlank
    @Size(max = 200)
    private String displayDesc;

    @NotNull
    @DecimalMin(value = "0.01", message = "Amount must be at least 0.01")
    private BigDecimal amountInRinggit;

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
}

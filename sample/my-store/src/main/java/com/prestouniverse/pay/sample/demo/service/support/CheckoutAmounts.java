package com.prestouniverse.pay.sample.demo.service.support;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CheckoutAmounts {

    private CheckoutAmounts() {
    }

    /** Converts MYR ringgit (major units) to sen (minor units) for the gateway {@code amount} field. */
    public static int toMinorUnits(BigDecimal amountInRinggit) {
        return amountInRinggit.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .intValueExact();
    }
}

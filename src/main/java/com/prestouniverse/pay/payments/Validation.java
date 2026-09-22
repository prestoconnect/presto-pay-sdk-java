package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

final class Validation {

    private Validation() {
    }

    static void requireNonNull(String field, Object value) {
        if (value == null) {
            throw new PrestoPayConfigException(field, field + " is required");
        }
    }

    static void requireMaxLength(String field, String value, int max) {
        if (value != null && value.length() > max) {
            throw new PrestoPayConfigException(field, field + " must be at most " + max + " characters");
        }
    }
}

package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class Validation {

    private Validation() {
    }

    static void requireNonNull(String field, Object value) {
        if (value == null) {
            throw new PrestoPayConfigException(field, field + " is required");
        }
    }

    static void requireNonBlank(String field, String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new PrestoPayConfigException(field, field + " is required");
        }
    }

    static void requirePositive(String field, Integer value) {
        if (value != null && value <= 0) {
            throw new PrestoPayConfigException(field, field + " must be greater than 0");
        }
    }

    static void requireNoNullElements(String field, Collection<?> values) {
        for (Object value : values) {
            if (value == null) {
                throw new PrestoPayConfigException(field, field + " must not contain null elements");
            }
        }
    }

    static <T> List<T> copyOf(Collection<? extends T> values) {
        return values == null ? new ArrayList<T>() : new ArrayList<T>(values);
    }

    static void requireMaxLength(String field, String value, int max) {
        if (value != null && value.length() > max) {
            throw new PrestoPayConfigException(field, field + " must be at most " + max + " characters");
        }
    }
}

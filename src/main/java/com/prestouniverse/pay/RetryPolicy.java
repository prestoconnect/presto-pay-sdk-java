package com.prestouniverse.pay;

import java.time.Duration;

public final class RetryPolicy {

    private final int maxRetries;
    private final Duration initialBackoff;

    private RetryPolicy(int maxRetries, Duration initialBackoff) {
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
    }

    public static RetryPolicy defaults() {
        return new RetryPolicy(2, Duration.ofMillis(200));
    }

    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ZERO);
    }

    public static RetryPolicy of(int maxRetries, Duration initialBackoff) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must be >= 0");
        }
        return new RetryPolicy(maxRetries, initialBackoff);
    }

    public int maxRetries() {
        return maxRetries;
    }

    public Duration backoffFor(int attempt) {
        int shift = Math.min(Math.max(attempt - 1, 0), 10);
        return initialBackoff.multipliedBy(1L << shift);
    }
}

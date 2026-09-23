package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

import java.time.Duration;

/**
 * Retry budget with exponential backoff. {@code query} is retried on transport errors and HTTP 5xx;
 * {@code init}, {@code reverse}, and {@code refund} are retried only when
 * {@code PrestoPayTransportException.requestNotSent()} is true.
 */
public final class RetryPolicy {

    private final int maxRetries;
    private final Duration initialBackoff;

    private RetryPolicy(int maxRetries, Duration initialBackoff) {
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
    }

    /** Two retries, starting at 200 ms and doubling. */
    public static RetryPolicy defaults() {
        return new RetryPolicy(2, Duration.ofMillis(200));
    }

    public static RetryPolicy none() {
        return new RetryPolicy(0, Duration.ZERO);
    }

    /**
     * @param maxRetries retries after the first attempt; must be at least 0
     * @param initialBackoff delay before the first retry, doubled for each later retry; must not be negative
     */
    public static RetryPolicy of(int maxRetries, Duration initialBackoff) {
        if (maxRetries < 0) {
            throw new PrestoPayConfigException("maxRetries", "maxRetries must be >= 0");
        }
        if (initialBackoff == null || initialBackoff.isNegative()) {
            throw new PrestoPayConfigException("initialBackoff", "initialBackoff must be zero or positive");
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

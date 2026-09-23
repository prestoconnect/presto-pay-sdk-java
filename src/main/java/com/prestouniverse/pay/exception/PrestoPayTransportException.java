package com.prestouniverse.pay.exception;

/**
 * Network, TLS, or timeout failure. When {@link #requestNotSent()} is {@code false} the gateway may have processed
 * the request; reconcile {@code init}, {@code reverse}, and {@code refund} with {@code query}.
 */
public class PrestoPayTransportException extends PrestoPayException {

    private static final long serialVersionUID = 1L;

    private final boolean requestNotSent;

    public PrestoPayTransportException(String message, Throwable cause, boolean requestNotSent) {
        super(message, cause);
        this.requestNotSent = requestNotSent;
    }

    public boolean requestNotSent() {
        return requestNotSent;
    }
}

package com.prestouniverse.pay.exception;

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

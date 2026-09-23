package com.prestouniverse.pay.exception;

/**
 * A signature could not be created or did not verify, or a verified webhook was rejected (wrong {@code mid} or
 * outside the freshness window). Treat as an authenticity failure.
 */
public class PrestoPaySignatureException extends PrestoPayException {

    private static final long serialVersionUID = 1L;

    public enum Side {
        REQUEST,
        RESPONSE,
        WEBHOOK
    }

    private final Side side;
    private final String canonicalString;

    public PrestoPaySignatureException(Side side, String message, String canonicalString) {
        super(message);
        this.side = side;
        this.canonicalString = canonicalString;
    }

    public PrestoPaySignatureException(Side side, String message, String canonicalString, Throwable cause) {
        super(message, cause);
        this.side = side;
        this.canonicalString = canonicalString;
    }

    public Side side() {
        return side;
    }

    public String canonicalString() {
        return canonicalString;
    }
}

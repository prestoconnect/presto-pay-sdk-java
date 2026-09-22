package com.prestouniverse.pay.exception;

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

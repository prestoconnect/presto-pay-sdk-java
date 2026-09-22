package com.prestouniverse.pay.exception;

public class PrestoPayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PrestoPayException(String message) {
        super(message);
    }

    public PrestoPayException(String message, Throwable cause) {
        super(message, cause);
    }
}

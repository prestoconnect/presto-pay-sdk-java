package com.prestouniverse.pay.exception;

/** Base class for all SDK errors. Unchecked; catch a subclass to handle a specific failure. */
public class PrestoPayException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public PrestoPayException(String message) {
        super(message);
    }

    public PrestoPayException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.prestouniverse.pay.exception;

public class PrestoPayConfigException extends PrestoPayException {

    private static final long serialVersionUID = 1L;

    private final String field;

    public PrestoPayConfigException(String field, String message) {
        super(message);
        this.field = field;
    }

    public PrestoPayConfigException(String field, String message, Throwable cause) {
        super(message, cause);
        this.field = field;
    }

    public String field() {
        return field;
    }
}

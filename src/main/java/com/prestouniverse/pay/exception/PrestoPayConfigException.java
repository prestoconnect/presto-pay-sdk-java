package com.prestouniverse.pay.exception;

/**
 * Invalid client configuration or request, detected before anything is sent. {@link #field()} names the offending
 * setting.
 */
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

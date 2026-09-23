package com.prestouniverse.pay.exception;

/**
 * A gateway response or webhook body could not be parsed: malformed JSON, unsupported value types, missing
 * required fields, or invalid field formats. Signature and authenticity failures use
 * {@link PrestoPaySignatureException} instead.
 *
 * <p>For {@link Source#RESPONSE} on {@code init}, {@code reverse}, or {@code refund}, the gateway may still have
 * processed the operation. Reconcile with {@code payments().query()} rather than retrying.
 */
public class PrestoPayResponseException extends PrestoPayException {

    private static final long serialVersionUID = 1L;

    public enum Source {
        RESPONSE,
        WEBHOOK
    }

    private final Source source;
    private final String rawBody;

    public PrestoPayResponseException(Source source, String message, String rawBody, Throwable cause) {
        super(message, cause);
        this.source = source;
        this.rawBody = rawBody;
    }

    public Source source() {
        return source;
    }

    public String rawBody() {
        return rawBody;
    }
}

package com.prestouniverse.pay.exception;

public class PrestoPayApiException extends PrestoPayException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;
    private final String errorCode;
    private final String errorMessage;
    private final boolean systemError;
    private final String rawBody;

    public PrestoPayApiException(int httpStatus, String errorCode, String errorMessage, boolean systemError,
            String rawBody) {
        super(buildMessage(httpStatus, errorCode, errorMessage, systemError));
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.systemError = systemError;
        this.rawBody = rawBody;
    }

    private static String buildMessage(int httpStatus, String errorCode, String errorMessage, boolean systemError) {
        String kind = systemError ? "system error" : "business error";
        return "Presto Connect " + kind + " (HTTP " + httpStatus + "): "
                + (errorCode != null ? errorCode : "?") + " " + (errorMessage != null ? errorMessage : "");
    }

    public int httpStatus() {
        return httpStatus;
    }

    public String errorCode() {
        return errorCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public boolean isSystemError() {
        return systemError;
    }

    public String rawBody() {
        return rawBody;
    }
}

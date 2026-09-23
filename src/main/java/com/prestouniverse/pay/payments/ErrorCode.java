package com.prestouniverse.pay.payments;

/**
 * Known gateway error codes, as returned by {@link
 * com.prestouniverse.pay.exception.PrestoPayApiException#errorCode()}. The gateway may return codes not listed
 * here.
 */
public final class ErrorCode {

    public static final String INVALID_REQUEST_PATH = "1001";
    public static final String INVALID_CONTENT_TYPE = "1002";
    public static final String MISSING_MASTER_MERCHANT_REFERENCE = "1003";
    public static final String INVALID_TIMESTAMP_FORMAT = "1004";
    public static final String EXCEEDED_VALIDITY_PERIOD = "1005";
    public static final String INVALID_SIGNATURE = "1006";
    public static final String SIGNATURE_VERIFICATION_FAILED = "1007";
    public static final String MISSING_AUTHORIZATION_HEADER = "1008";
    public static final String INVALID_AUTHORIZATION_HEADER = "1009";
    public static final String INVALID_ACCESS_TOKEN = "1010";
    public static final String ACCESS_TOKEN_VALIDATION_ERROR = "1011";
    public static final String INVALID_ACCESS_TOKEN_OWNERSHIP = "1012";
    public static final String OAUTH_RESOURCE_CONFIG_ERROR = "1013";
    public static final String MISSING_OAUTH_SCOPE = "1014";
    public static final String OAUTH_SERVICE_UNAVAILABLE = "1015";

    public static final String MERCHANT_GENERAL_ERROR = "1100";
    public static final String MERCHANT_INVALID_INPUT = "1101";
    public static final String INVALID_MID = "1102";
    public static final String MASTER_MERCHANT_INFO_RETRIEVAL_FAILED = "1103";
    public static final String MASTER_MERCHANT_INFO_SERVICE_UNAVAILABLE = "1104";
    public static final String MERCHANT_ONBOARD_PROCESSING_FAILED = "1105";
    public static final String INVALID_MERCHANT_REFERENCE = "1106";
    public static final String MERCHANT_DOCUMENT_UPLOAD_FAILED = "1107";
    public static final String MISSING_MERCHANT_DOCUMENT = "1108";
    public static final String MERCHANT_RECORD_EXISTS = "1109";
    public static final String MERCHANT_PROFILE_NOT_FOUND = "1110";
    public static final String INVALID_MERCHANT_TXN_TYPE = "1111";
    public static final String MERCHANT_FILE_RETRY_LIMIT_EXCEEDED = "1112";
    public static final String MERCHANT_REJECTED = "1113";

    public static final String PAYMENT_GENERAL_ERROR = "1200";
    public static final String PAYMENT_INVALID_INPUT = "1201";
    public static final String PAYMENT_INIT_FAILED = "1202";
    public static final String DUPLICATE_TXN_REF_NUM = "1203";
    public static final String QR_VALUE_NOT_RECOGNISED = "1204";
    public static final String QR_VALUE_NOT_BOUND = "1205";
    public static final String QR_VALUE_TOTP_EXPIRED = "1206";
    public static final String QR_VALUE_VALIDATION_FAILED = "1207";
    public static final String QR_VALUE_INVALID_TOTP_SECRET = "1208";
    public static final String QR_VALUE_INVALID_TOTP = "1209";
    public static final String QR_VALUE_INVALID_PREFIX = "1210";
    public static final String QR_PAYMENT_SUSPENDED = "1211";
    public static final String PAYMENT_NOT_FOUND = "1212";
    public static final String INVALID_PAYMENT_STATUS_FOR_AUTHORISATION = "1213";
    public static final String AUTHORISATION_GENERAL_ERROR = "1214";
    public static final String QR_VALUE_ALREADY_USED = "1215";
    public static final String INVALID_USER = "1216";
    public static final String QUERY_ACCESS_DENIED = "1217";
    public static final String PAYMENT_QUERY_FAILED = "1218";
    public static final String INVALID_PAYMENT_STATUS_FOR_REVERSAL = "1219";
    public static final String REVERSAL_NOT_ALLOWED_SETTLED = "1220";
    public static final String REVERSAL_GRACE_PERIOD_ENDED = "1221";
    public static final String REFUND_TO_ACCOUNT_FAILED = "1222";
    public static final String REVERSAL_FAILED = "1223";
    public static final String REVERSAL_ALREADY_IN_PROGRESS = "1224";
    public static final String INVALID_PAYMENT_METHOD = "1225";
    public static final String REFUND_ALREADY_IN_PROGRESS = "1226";
    public static final String INVALID_PAYMENT_STATUS_FOR_REFUND = "1227";
    public static final String REFUND_GRACE_PERIOD_ENDED = "1228";
    public static final String INSUFFICIENT_UNSETTLED_AMOUNT = "1229";
    public static final String REFUND_FAILED = "1230";
    public static final String PAYMENT_LIMIT_EXCEEDED = "1231";
    public static final String INVALID_SESSION_VALIDITY_PERIOD = "1232";
    public static final String INVALID_SESSION_VALIDITY_FORMAT = "1233";
    public static final String PAYMENT_METHOD_MISMATCH = "1234";
    public static final String REFUND_AMOUNT_EXCEEDS_TRANSACTION = "1235";
    public static final String REFUNDABLE_AMOUNT_EXCEEDED = "1236";

    public static final String USER_GENERAL_ERROR = "1400";
    public static final String INVALID_USER_TOKEN_FORMAT = "1401";
    public static final String USER_TOKEN_NOT_FOUND = "1402";
    public static final String USER_REFERENCE_RETRIEVAL_FAILED = "1403";
    public static final String USER_PROFILE_RETRIEVAL_FAILED = "1404";
    public static final String USER_INFO_RETRIEVAL_FAILED = "1405";

    private ErrorCode() {
    }
}

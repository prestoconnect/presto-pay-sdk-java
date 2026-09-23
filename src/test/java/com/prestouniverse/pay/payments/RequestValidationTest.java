package com.prestouniverse.pay.payments;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestValidationTest {

    private static final String MRN = "PM240110XDSFC";

    @Test
    void everyRequestRequiresMerchantRefNum() {
        assertRejects("merchantRefNum", () -> PaymentInitRequest.builder()
                .txnType(TxnType.QrPay)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .build());
        assertRejects("merchantRefNum", () -> PaymentQueryRequest.builder()
                .paymentRefNum("PP1")
                .build());
        assertRejects("merchantRefNum", () -> PaymentReverseRequest.builder()
                .paymentRefNum("PP1")
                .reversalRefNum("REV1")
                .build());
        assertRejects("merchantRefNum", () -> PaymentRefundRequest.builder()
                .paymentRefNum("PP1")
                .refundRefNum("RFD1")
                .remark("remark")
                .build());
    }

    @Test
    void blankMerchantRefNumIsRejected() {
        assertRejects("merchantRefNum", () -> query().merchantRefNum("").paymentRefNum("PP1").build());
    }

    @Test
    void initRequiresTxnType() {
        assertRejects("txnType", () -> init()
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .build());
    }

    @Test
    void initRejectsQrValueAndPayerRefNumTogether() {
        assertRejects("qrValue", () -> init()
                .txnType(TxnType.QrPay)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .qrValue("qr")
                .payerRefNum("payer")
                .build());
    }

    @Test
    void initRequiresCurrencyCodeWhenAmountIsSet() {
        assertRejects("currencyCode", () -> init()
                .txnType(TxnType.QrPay)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .amount(100)
                .build());
    }

    @Test
    void webPayRequiresRedirectUrl() {
        assertRejects("redirectUrl", () -> init()
                .txnType(TxnType.WebPay)
                .txnRefNum("TXN1")
                .displayDesc("desc")
                .build());
    }

    @Test
    void initRejectsTxnRefNumOverMaxLength() {
        String tooLong = repeat("a", 51);
        assertRejects("txnRefNum", () -> init()
                .txnType(TxnType.QrPay)
                .txnRefNum(tooLong)
                .displayDesc("desc")
                .build());
    }

    @Test
    void initRejectsNonPositiveAmount() {
        assertRejects("amount", () -> qrInit().amount(0).currencyCode("MYR").build());
        assertRejects("amount", () -> qrInit().amount(-100).currencyCode("MYR").build());
    }

    @Test
    void initRejectsNullListElements() {
        LineItem item = LineItem.builder().itemDesc("Coffee").quantity(1).unitAmount(100).totalAmount(100).build();
        assertRejects("items", () -> qrInit().items(item, null).build());
        assertRejects("allowedPaymentMethods", () -> qrInit()
                .allowedPaymentMethods(Arrays.asList(PaymentMethod.Card, null))
                .build());
    }

    @Test
    void initListSettersTreatNullAsEmptyAndCopyTheInput() {
        List<String> methods = new ArrayList<>(Arrays.asList(PaymentMethod.Card));
        PaymentInitRequest request = qrInit()
                .items((LineItem[]) null)
                .allowedPaymentMethods(methods)
                .build();
        methods.add(PaymentMethod.Wallet);

        JsonObject json = request.toJson();
        assertFalse(json.has("itemList"));
        assertEquals("[\"" + PaymentMethod.Card + "\"]", json.get("allowedPaymentMethods"));
    }

    @Test
    void refundRejectsNonPositiveAmountAndLongNotifyUrl() {
        assertRejects("amount", () -> refund().amount(0).build());
        assertRejects("notifyUrl", () -> refund().notifyUrl("https://x/" + repeat("a", 250)).build());
    }

    @Test
    void queryRequiresEitherRefNum() {
        assertRejects("paymentRefNum", () -> query().build());
    }

    @Test
    void reverseRequiresReversalRefNum() {
        assertRejects("reversalRefNum", () -> PaymentReverseRequest.builder()
                .merchantRefNum(MRN)
                .paymentRefNum("PP1")
                .build());
    }

    @Test
    void refundRequiresRemark() {
        assertRejects("remark", () -> PaymentRefundRequest.builder()
                .merchantRefNum(MRN)
                .paymentRefNum("PP1")
                .refundRefNum("RFD1")
                .build());
    }

    private static PaymentInitRequest.Builder init() {
        return PaymentInitRequest.builder().merchantRefNum(MRN);
    }

    private static PaymentInitRequest.Builder qrInit() {
        return init().txnType(TxnType.QrPay).txnRefNum("TXN1").displayDesc("desc");
    }

    private static PaymentRefundRequest.Builder refund() {
        return PaymentRefundRequest.builder()
                .merchantRefNum(MRN)
                .paymentRefNum("PP1")
                .refundRefNum("RFD1")
                .remark("remark");
    }

    private static PaymentQueryRequest.Builder query() {
        return PaymentQueryRequest.builder().merchantRefNum(MRN);
    }

    private static void assertRejects(String field, Executable build) {
        PrestoPayConfigException exception = assertThrows(PrestoPayConfigException.class, build);
        assertEquals(field, exception.field());
    }

    private static String repeat(String s, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(s);
        }
        return builder.toString();
    }
}

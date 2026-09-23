package com.prestouniverse.pay.crypto;

import com.prestouniverse.pay.internal.Canonicalization;
import com.prestouniverse.pay.internal.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalizerTest {

    @Test
    void specWorkedExample() {
        JsonObject body = new JsonObject();
        body.put("mid", "PW2401XH9KCX");
        body.put("prestoMrn", "PM240110XDSFC");
        body.put("txnType", "WebPay");
        body.put("txnRefNum", "TXN10001");
        body.put("displayDesc", "Order #12345");
        body.put("amount", 1200);
        body.put("currencyCode", "MYR");
        body.put("notifyUrl", "https://merchant.example.com/webhook/notify");
        body.put("redirectUrl", "https://merchant.example.com/redirect/TXN10001");
        body.put("ts", "20250423104500.000");

        String expected = "1200:MYR:Order #12345:PW2401XH9KCX:https://merchant.example.com/webhook/notify:"
                + "PM240110XDSFC:https://merchant.example.com/redirect/TXN10001:20250423104500.000:TXN10001:WebPay";

        assertEquals(expected, Canonicalization.canonicalize(body));
    }

    @Test
    void canonicalizeJsonMatchesObjectCanonicalize() {
        String json = "{\"mid\":\"PW2401XH9KCX\",\"amount\":1200,\"ts\":\"20250423104500.000\"}";
        JsonObject body = JsonObject.parse(json);
        assertEquals(Canonicalization.canonicalize(body), Canonicalizer.canonicalizeJson(json));
    }

    @Test
    void businessErrorBodyFromPresto() {
        JsonObject body = new JsonObject();
        body.put("success", false);
        body.put("ts", "20260922135741.041");
        body.put("errorCode", "1201");
        body.put("errorMessage", "Invalid input.");
        body.put("signature", "B7De0lL7Nh3SfNPE0+xK6Yu4Rk7nd3dqQ5EpvGpSLw4...");

        assertEquals("1201:Invalid input.:false:20260922135741.041", Canonicalization.canonicalize(body));
    }

    @Test
    void nullFieldRendersAsEmptyStringButKeepsSeparator() {
        JsonObject body = new JsonObject();
        body.put("a", "x");
        body.putNull("b");
        body.put("c", "y");

        assertEquals("x::y", Canonicalization.canonicalize(body));
    }

    @Test
    void absentFieldIsSimplyNotInTheString() {
        JsonObject withField = new JsonObject();
        withField.put("a", "x");
        withField.putNull("b");
        withField.put("c", "y");

        JsonObject withoutField = new JsonObject();
        withoutField.put("a", "x");
        withoutField.put("c", "y");

        assertEquals("x::y", Canonicalization.canonicalize(withField));
        assertEquals("x:y", Canonicalization.canonicalize(withoutField));
    }

    @Test
    void emptyStringFieldRendersAsEmptyString() {
        JsonObject body = new JsonObject();
        body.put("additionalData", "");
        body.put("mid", "PW2401XH9KCX");

        assertEquals(":PW2401XH9KCX", Canonicalization.canonicalize(body));
    }

    @Test
    void arrayFieldRendersAsCompactJsonText() {
        JsonObject body = new JsonObject();
        body.putArray("allowedPaymentMethods").add("Wallet").add("Card");
        body.put("mid", "PW2401XH9KCX");

        assertEquals("[\"Wallet\",\"Card\"]:PW2401XH9KCX", Canonicalization.canonicalize(body));
    }

    @Test
    void jsonEncodedStringArrayFieldMatchesNativeArrayCanonicalValue() {
        JsonObject asArray = new JsonObject();
        asArray.putArray("allowedPaymentMethods").add("Wallet").add("Card");
        asArray.put("mid", "PW2401XH9KCX");

        JsonObject asString = new JsonObject();
        asString.put("allowedPaymentMethods", "[\"Wallet\",\"Card\"]");
        asString.put("mid", "PW2401XH9KCX");

        assertEquals(Canonicalization.canonicalize(asArray), Canonicalization.canonicalize(asString));
    }

    @Test
    void jsonStringFieldIsTreatedAsAnOrdinaryString() {
        JsonObject body = new JsonObject();
        body.put("paymentDetails", "[{\"method\":\"Wallet\",\"amount\":5000}]");
        body.put("mid", "PW2401XH9KCX");

        assertEquals("PW2401XH9KCX:[{\"method\":\"Wallet\",\"amount\":5000}]", Canonicalization.canonicalize(body));
    }

    @Test
    void signatureFieldIsExcludedFromTheCanonicalString() {
        JsonObject body = new JsonObject();
        body.put("mid", "PW2401XH9KCX");
        body.put("signature", "shouldNotAppear");

        assertEquals("PW2401XH9KCX", Canonicalization.canonicalize(body));
    }

    @Test
    void chineseCharactersAppearRawInTheCanonicalString() {
        JsonObject body = new JsonObject();
        body.put("mid", "PW2401XH9KCX");
        body.put("displayDesc", "訂單 #12345 測試");

        assertEquals("訂單 #12345 測試:PW2401XH9KCX", Canonicalization.canonicalize(body));
    }

    @Test
    void embeddedNewlineIsCarriedThroughVerbatim() {
        JsonObject body = new JsonObject();
        body.put("mid", "PW2401XH9KCX");
        body.put("remark", "line one\r\nline two");

        assertEquals("PW2401XH9KCX:line one\r\nline two", Canonicalization.canonicalize(body));
    }

    @Test
    void embeddedColonInAValueDoesNotConfuseFieldJoining() {
        JsonObject body = new JsonObject();
        body.put("a", "x");
        body.put("b", "https://merchant.example.com/x:y:z");

        assertEquals("x:https://merchant.example.com/x:y:z", Canonicalization.canonicalize(body));
    }

    @Test
    void nestedObjectIsRejected() {
        JsonObject body = new JsonObject();
        body.putObject("nested").put("x", 1);

        assertThrows(IllegalArgumentException.class, () -> Canonicalization.canonicalize(body));
    }
}

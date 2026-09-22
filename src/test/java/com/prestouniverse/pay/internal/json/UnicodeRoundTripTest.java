package com.prestouniverse.pay.internal.json;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnicodeRoundTripTest {

    @Test
    void chineseCharactersSurviveWriteThenParse() {
        String original = "订单 #12345 测试";
        JsonObject body = new JsonObject();
        body.put("displayDesc", original);

        String json = body.toJson();
        JsonObject parsed = JsonObject.parse(json);

        assertEquals(original, parsed.get("displayDesc"));
    }

    @Test
    void chineseCharactersAreWrittenRawNotEscaped() {
        JsonObject body = new JsonObject();
        body.put("displayDesc", "订单");

        String json = body.toJson();

        assertTrue(json.contains("订单"), "expected raw UTF-8 characters in the JSON text, got: " + json);
        assertTrue(json.getBytes(StandardCharsets.UTF_8).length > json.length(),
                "expected multi-byte UTF-8 encoding for CJK characters");
    }

    @Test
    void chineseCharactersSurviveActualUtf8ByteRoundTrip() {
        String original = "訂單備註：客戶已付款，请尽快处理退款";
        JsonObject body = new JsonObject();
        body.put("remark", original);

        byte[] wireBytes = body.toJson().getBytes(StandardCharsets.UTF_8);
        String receivedText = new String(wireBytes, StandardCharsets.UTF_8);
        JsonObject parsed = JsonObject.parse(receivedText);

        assertEquals(original, parsed.get("remark"));
    }

    @Test
    void supplementaryPlaneCharacterSurvivesRawInText() {
        String original = "receipt 😀 done";
        JsonObject body = new JsonObject();
        body.put("note", original);

        JsonObject parsed = JsonObject.parse(body.toJson());

        assertEquals(original, parsed.get("note"));
    }

    @Test
    void escapedSurrogatePairParsesToTheSameCharacter() {
        JsonObject parsed = JsonObject.parse("{\"note\":\"\\uD83D\\uDE00\"}");

        assertEquals("😀", parsed.get("note"));
    }

    @Test
    void escapedUnicodeAndRawUnicodeParseToTheSameString() {
        JsonObject viaEscape = JsonObject.parse("{\"a\":\"\\u8a02\\u55ae\"}");
        JsonObject viaRaw = JsonObject.parse("{\"a\":\"訂單\"}");

        assertEquals(viaRaw.get("a"), viaEscape.get("a"));
    }

    @Test
    void arrayOfChineseStringsRoundTrips() {
        JsonObject body = new JsonObject();
        body.putArray("tags").add("錢包").add("信用卡");

        JsonObject parsed = JsonObject.parse(body.toJson());
        JsonArray tags = (JsonArray) parsed.get("tags");

        assertEquals("錢包", tags.get(0));
        assertEquals("信用卡", tags.get(1));
    }
}

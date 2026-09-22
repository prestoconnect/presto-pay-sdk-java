package com.prestouniverse.pay.internal.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlCharacterRoundTripTest {

    @Test
    void newlineIsEscapedOnWrite() {
        JsonObject body = new JsonObject();
        body.put("remark", "line one\nline two");

        String json = body.toJson();

        assertTrue(json.contains("\\n"), "expected an escaped \\n in the JSON text, got: " + json);
        assertTrue(json.indexOf('\n') < 0, "raw newline must never appear unescaped in written JSON");
    }

    @Test
    void windowsStyleCrlfSurvivesWriteThenParse() {
        String original = "line one\r\nline two\r\nline three";
        JsonObject body = new JsonObject();
        body.put("remark", original);

        JsonObject parsed = JsonObject.parse(body.toJson());

        assertEquals(original, parsed.get("remark"));
    }

    @Test
    void tabAndCarriageReturnAreEscapedAndRestoredExactly() {
        String original = "a\tb\rc";
        JsonObject body = new JsonObject();
        body.put("value", original);

        JsonObject parsed = JsonObject.parse(body.toJson());

        assertEquals(original, parsed.get("value"));
    }

    @Test
    void escapedNewlineInIncomingJsonParsesToARealNewlineCharacter() {
        JsonObject parsed = JsonObject.parse("{\"remark\":\"line one\\nline two\"}");

        assertEquals("line one\nline two", parsed.get("remark"));
    }

    @Test
    void literalUnescapedNewlineInIncomingJsonIsAcceptedLeniently() {
        String jsonWithRawNewline = "{\"remark\":\"line one\nline two\"}";

        JsonObject parsed = JsonObject.parse(jsonWithRawNewline);

        assertEquals("line one\nline two", parsed.get("remark"));
    }

    @Test
    void multiLineValueInsideAnArrayRoundTrips() {
        JsonObject body = new JsonObject();
        body.putArray("notes").add("first\nsecond").add("third\r\nfourth");

        JsonObject parsed = JsonObject.parse(body.toJson());
        JsonArray notes = (JsonArray) parsed.get("notes");

        assertEquals("first\nsecond", notes.get(0));
        assertEquals("third\r\nfourth", notes.get(1));
    }
}

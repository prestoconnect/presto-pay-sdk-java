package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.internal.json.JsonObject;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JsonCodecTest {

    @Test
    void intOutsideIntRangeIsRejectedInsteadOfTruncated() {
        JsonObject node = JsonCodec.parseObject("{\"amount\":4294968496}");

        assertThrows(IllegalArgumentException.class, () -> JsonCodec.optInt(node, "amount"));
    }

    @Test
    void moderatelyNestedJsonParses() {
        JsonObject node = JsonCodec.parseObject("{\"a\":{\"b\":[[{\"c\":1}]]}}");

        assertEquals(1, node.fieldNames().size());
    }

    @Test
    void deeplyNestedJsonIsRejectedWithoutExhaustingTheStack() {
        StringBuilder json = new StringBuilder("{\"a\":");
        for (int i = 0; i < 100_000; i++) {
            json.append('[');
        }

        assertThrows(IllegalArgumentException.class, () -> JsonCodec.parseObject(json.toString()));
    }
}

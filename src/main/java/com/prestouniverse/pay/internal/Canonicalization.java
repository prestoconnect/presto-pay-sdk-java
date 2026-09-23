package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.internal.json.JsonArray;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Canonicalization {

    private static final String SIGNATURE_FIELD = "signature";

    private Canonicalization() {
    }

    /**
     * Sorts keys (excluding {@code signature}) and joins their values with {@code :}; {@code null} renders empty.
     *
     * @throws IllegalArgumentException if a value is not a string, boolean, integer, or array
     */
    public static String canonicalize(JsonObject body) {
        List<String> keys = new ArrayList<>();
        for (String key : body.fieldNames()) {
            if (!SIGNATURE_FIELD.equals(key)) {
                keys.add(key);
            }
        }
        Collections.sort(keys);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < keys.size(); i++) {
            if (i > 0) {
                result.append(':');
            }
            String key = keys.get(i);
            result.append(render(key, body.get(key)));
        }
        return result.toString();
    }

    private static String render(String key, Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String) {
            return (String) value;
        }
        if (value instanceof Boolean) {
            return ((Boolean) value) ? "true" : "false";
        }
        if (value instanceof Integer || value instanceof Long) {
            return value.toString();
        }
        if (value instanceof JsonArray) {
            return value.toString();
        }
        throw new IllegalArgumentException(
                "Cannot canonicalize field '" + key + "': unsupported value type " + value.getClass());
    }
}

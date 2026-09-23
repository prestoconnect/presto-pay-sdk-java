package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.internal.json.JsonArray;
import com.prestouniverse.pay.internal.json.JsonObject;

import java.util.ArrayList;
import java.util.List;

public final class JsonCodec {

    private JsonCodec() {
    }

    public static JsonObject newObject() {
        return new JsonObject();
    }

    public static JsonArray newArray() {
        return new JsonArray();
    }

    public static JsonObject parseObject(String json) {
        try {
            return JsonObject.parse(json);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage(), e);
        }
    }

    public static List<JsonObject> parseObjectArray(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        JsonArray array;
        try {
            array = JsonArray.parse(json);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Malformed JSON: " + e.getMessage(), e);
        }
        List<JsonObject> result = new ArrayList<>();
        for (Object element : array) {
            if (!(element instanceof JsonObject)) {
                throw new IllegalArgumentException("Expected a JSON array of objects");
            }
            result.add((JsonObject) element);
        }
        return result;
    }

    public static String write(JsonObject node) {
        return node.toJson();
    }

    public static void putIfPresent(JsonObject node, String field, String value) {
        if (value != null) {
            node.put(field, value);
        }
    }

    public static void putIfPresent(JsonObject node, String field, Integer value) {
        if (value != null) {
            node.put(field, value.intValue());
        }
    }

    /**
     * Writes a JSON array of strings as a single string field (same wire shape as {@code itemList}).
     * Presto Connect maps {@code allowedPaymentMethods} to a {@code String} on init, not a JSON array.
     */
    public static void putStringArray(JsonObject node, String field, List<String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }
        JsonArray array = newArray();
        for (String value : values) {
            array.add(value);
        }
        node.put(field, array.toString());
    }

    public static String text(JsonObject node, String field) {
        Object value = node.get(field);
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : String.valueOf(value);
    }

    public static String requiredText(JsonObject node, String field) {
        String value = text(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    public static Integer optInt(JsonObject node, String field) {
        Object value = node.get(field);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Long) {
            throw new IllegalArgumentException("Field '" + field + "' is out of int range: " + value);
        }
        throw new IllegalArgumentException("Field '" + field + "' is not an integer");
    }

    public static int requiredInt(JsonObject node, String field) {
        Integer value = optInt(node, field);
        if (value == null) {
            throw new IllegalArgumentException("Missing required field: " + field);
        }
        return value;
    }

    public static boolean optBoolean(JsonObject node, String field, boolean defaultValue) {
        Object value = node.get(field);
        if (value == null) {
            return defaultValue;
        }
        if (!(value instanceof Boolean)) {
            throw new IllegalArgumentException("Field '" + field + "' is not a boolean");
        }
        return (Boolean) value;
    }
}

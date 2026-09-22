package com.prestouniverse.pay.internal.json;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class JsonObject {

    private final Map<String, Object> fields = new LinkedHashMap<>();

    public static JsonObject parse(String json) {
        Object value = JsonParser.parse(json);
        if (!(value instanceof JsonObject)) {
            throw new IllegalArgumentException("Expected a JSON object");
        }
        return (JsonObject) value;
    }

    public JsonObject put(String key, String value) {
        fields.put(key, value);
        return this;
    }

    public JsonObject put(String key, int value) {
        fields.put(key, value);
        return this;
    }

    public JsonObject put(String key, boolean value) {
        fields.put(key, value);
        return this;
    }

    public JsonObject putNull(String key) {
        fields.put(key, null);
        return this;
    }

    public JsonArray putArray(String key) {
        JsonArray array = new JsonArray();
        fields.put(key, array);
        return array;
    }

    public JsonObject putObject(String key) {
        JsonObject object = new JsonObject();
        fields.put(key, object);
        return object;
    }

    public Object get(String key) {
        return fields.get(key);
    }

    public boolean has(String key) {
        return fields.containsKey(key);
    }

    public Set<String> fieldNames() {
        return Collections.unmodifiableSet(fields.keySet());
    }

    public JsonObject remove(String key) {
        fields.remove(key);
        return this;
    }

    public JsonObject deepCopy() {
        JsonObject copy = new JsonObject();
        for (Map.Entry<String, Object> entry : fields.entrySet()) {
            copy.fields.put(entry.getKey(), deepCopyValue(entry.getValue()));
        }
        return copy;
    }

    private static Object deepCopyValue(Object value) {
        if (value instanceof JsonObject) {
            return ((JsonObject) value).deepCopy();
        }
        if (value instanceof JsonArray) {
            return ((JsonArray) value).deepCopy();
        }
        return value;
    }

    public String toJson() {
        return JsonWriter.write(this);
    }

    void putRaw(String key, Object value) {
        fields.put(key, value);
    }

    @Override
    public String toString() {
        return toJson();
    }
}

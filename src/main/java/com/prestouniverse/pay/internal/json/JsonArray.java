package com.prestouniverse.pay.internal.json;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class JsonArray implements Iterable<Object> {

    private final List<Object> elements = new ArrayList<>();

    public static JsonArray parse(String json) {
        Object value = JsonParser.parse(json);
        if (!(value instanceof JsonArray)) {
            throw new IllegalArgumentException("Expected a JSON array");
        }
        return (JsonArray) value;
    }

    public JsonArray add(String value) {
        elements.add(value);
        return this;
    }

    public JsonArray add(JsonObject value) {
        elements.add(value);
        return this;
    }

    public int size() {
        return elements.size();
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }

    public Object get(int index) {
        return elements.get(index);
    }

    public JsonArray deepCopy() {
        JsonArray copy = new JsonArray();
        for (Object element : elements) {
            copy.elements.add(deepCopyValue(element));
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

    void addRaw(Object value) {
        elements.add(value);
    }

    @Override
    public Iterator<Object> iterator() {
        return Collections.unmodifiableList(elements).iterator();
    }

    @Override
    public String toString() {
        return JsonWriter.write(this);
    }
}

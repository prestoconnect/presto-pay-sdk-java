package com.prestouniverse.pay.internal.json;

final class JsonWriter {

    private JsonWriter() {
    }

    static String write(Object value) {
        StringBuilder result = new StringBuilder();
        writeValue(result, value);
        return result.toString();
    }

    private static void writeValue(StringBuilder result, Object value) {
        if (value == null) {
            result.append("null");
        } else if (value instanceof String) {
            writeString(result, (String) value);
        } else if (value instanceof Boolean) {
            result.append(((Boolean) value) ? "true" : "false");
        } else if (value instanceof Integer || value instanceof Long) {
            result.append(value.toString());
        } else if (value instanceof JsonObject) {
            writeObject(result, (JsonObject) value);
        } else if (value instanceof JsonArray) {
            writeArray(result, (JsonArray) value);
        } else {
            throw new IllegalArgumentException("Cannot serialize value of type " + value.getClass());
        }
    }

    private static void writeObject(StringBuilder result, JsonObject object) {
        result.append('{');
        boolean first = true;
        for (String key : object.fieldNames()) {
            if (!first) {
                result.append(',');
            }
            first = false;
            writeString(result, key);
            result.append(':');
            writeValue(result, object.get(key));
        }
        result.append('}');
    }

    private static void writeArray(StringBuilder result, JsonArray array) {
        result.append('[');
        for (int i = 0; i < array.size(); i++) {
            if (i > 0) {
                result.append(',');
            }
            writeValue(result, array.get(i));
        }
        result.append(']');
    }

    private static void writeString(StringBuilder result, String value) {
        result.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':
                    result.append("\\\"");
                    break;
                case '\\':
                    result.append("\\\\");
                    break;
                case '\b':
                    result.append("\\b");
                    break;
                case '\f':
                    result.append("\\f");
                    break;
                case '\n':
                    result.append("\\n");
                    break;
                case '\r':
                    result.append("\\r");
                    break;
                case '\t':
                    result.append("\\t");
                    break;
                default:
                    if (c < 0x20) {
                        result.append(String.format("\\u%04x", (int) c));
                    } else {
                        result.append(c);
                    }
            }
        }
        result.append('"');
    }
}

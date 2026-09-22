package com.prestouniverse.pay.internal.json;

final class JsonParser {

    private final String text;
    private int pos;

    private JsonParser(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        JsonParser parser = new JsonParser(text);
        parser.skipWhitespace();
        Object value = parser.parseValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw new IllegalArgumentException("Unexpected trailing content at position " + parser.pos);
        }
        return value;
    }

    private Object parseValue() {
        char c = peek();
        switch (c) {
            case '{':
                return parseObject();
            case '[':
                return parseArray();
            case '"':
                return parseString();
            case 't':
                return parseLiteral("true", Boolean.TRUE);
            case 'f':
                return parseLiteral("false", Boolean.FALSE);
            case 'n':
                return parseLiteral("null", null);
            default:
                if (c == '-' || Character.isDigit(c)) {
                    return parseNumber();
                }
                throw new IllegalArgumentException("Unexpected character '" + c + "' at position " + pos);
        }
    }

    private JsonObject parseObject() {
        JsonObject object = new JsonObject();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            object.putRaw(key, parseValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == '}') {
                break;
            }
            throw new IllegalArgumentException("Expected ',' or '}' at position " + (pos - 1));
        }
        return object;
    }

    private JsonArray parseArray() {
        JsonArray array = new JsonArray();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return array;
        }
        while (true) {
            skipWhitespace();
            array.addRaw(parseValue());
            skipWhitespace();
            char c = next();
            if (c == ',') {
                continue;
            }
            if (c == ']') {
                break;
            }
            throw new IllegalArgumentException("Expected ',' or ']' at position " + (pos - 1));
        }
        return array;
    }

    private String parseString() {
        expect('"');
        StringBuilder result = new StringBuilder();
        while (true) {
            if (pos >= text.length()) {
                throw new IllegalArgumentException("Unterminated string");
            }
            char c = text.charAt(pos++);
            if (c == '"') {
                break;
            }
            if (c != '\\') {
                result.append(c);
                continue;
            }
            if (pos >= text.length()) {
                throw new IllegalArgumentException("Unterminated escape sequence");
            }
            char escape = text.charAt(pos++);
            switch (escape) {
                case '"':
                    result.append('"');
                    break;
                case '\\':
                    result.append('\\');
                    break;
                case '/':
                    result.append('/');
                    break;
                case 'b':
                    result.append('\b');
                    break;
                case 'f':
                    result.append('\f');
                    break;
                case 'n':
                    result.append('\n');
                    break;
                case 'r':
                    result.append('\r');
                    break;
                case 't':
                    result.append('\t');
                    break;
                case 'u':
                    result.append(parseUnicodeEscape());
                    break;
                default:
                    throw new IllegalArgumentException("Invalid escape character '" + escape + "'");
            }
        }
        return result.toString();
    }

    private char parseUnicodeEscape() {
        if (pos + 4 > text.length()) {
            throw new IllegalArgumentException("Invalid unicode escape at position " + pos);
        }
        String hex = text.substring(pos, pos + 4);
        pos += 4;
        try {
            return (char) Integer.parseInt(hex, 16);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid unicode escape '\\u" + hex + "'", e);
        }
    }

    private Object parseNumber() {
        int start = pos;
        if (peek() == '-') {
            pos++;
        }
        consumeDigits();
        boolean floating = false;
        if (pos < text.length() && text.charAt(pos) == '.') {
            floating = true;
            pos++;
            consumeDigits();
        }
        if (pos < text.length() && (text.charAt(pos) == 'e' || text.charAt(pos) == 'E')) {
            floating = true;
            pos++;
            if (pos < text.length() && (text.charAt(pos) == '+' || text.charAt(pos) == '-')) {
                pos++;
            }
            consumeDigits();
        }
        String number = text.substring(start, pos);
        if (floating) {
            return Double.parseDouble(number);
        }
        try {
            long value = Long.parseLong(number);
            return value >= Integer.MIN_VALUE && value <= Integer.MAX_VALUE ? (Object) (int) value : (Object) value;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid number '" + number + "' at position " + start, e);
        }
    }

    private void consumeDigits() {
        while (pos < text.length() && Character.isDigit(text.charAt(pos))) {
            pos++;
        }
    }

    private Object parseLiteral(String literal, Object value) {
        if (pos + literal.length() > text.length() || !text.regionMatches(pos, literal, 0, literal.length())) {
            throw new IllegalArgumentException("Invalid literal at position " + pos);
        }
        pos += literal.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw new IllegalArgumentException("Unexpected end of input");
        }
        return text.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) {
        char actual = next();
        if (actual != expected) {
            throw new IllegalArgumentException(
                    "Expected '" + expected + "' but found '" + actual + "' at position " + (pos - 1));
        }
    }
}

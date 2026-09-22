package com.prestouniverse.pay.http;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public final class HttpResponse {

    private final int status;
    private final Map<String, String> headers;
    private final String body;

    private HttpResponse(int status, Map<String, String> headers, String body) {
        this.status = status;
        Map<String, String> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.putAll(headers);
        this.headers = Collections.unmodifiableMap(caseInsensitive);
        this.body = body;
    }

    public static HttpResponse of(int status, Map<String, String> headers, String body) {
        return new HttpResponse(status, headers, body);
    }

    public int status() {
        return status;
    }

    public String header(String name) {
        return headers.get(name);
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }
}

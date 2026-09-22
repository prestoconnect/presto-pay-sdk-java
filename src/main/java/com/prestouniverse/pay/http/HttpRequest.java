package com.prestouniverse.pay.http;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class HttpRequest {

    private final String url;
    private final Map<String, String> headers;
    private final String body;

    private HttpRequest(String url, Map<String, String> headers, String body) {
        this.url = url;
        this.headers = Collections.unmodifiableMap(new LinkedHashMap<>(headers));
        this.body = body;
    }

    public static HttpRequest post(String url, Map<String, String> headers, String body) {
        return new HttpRequest(url, headers, body);
    }

    public String url() {
        return url;
    }

    public Map<String, String> headers() {
        return headers;
    }

    public String body() {
        return body;
    }
}

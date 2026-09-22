package com.prestouniverse.pay;

public final class Environment {

    public static final Environment STAGING = new Environment("https://presto-stg-ext.enovax.com");
    public static final Environment PRODUCTION = new Environment("https://pay-ext.prestouniverse.com");

    private final String baseUrl;

    private Environment(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static Environment custom(String baseUrl) {
        if (baseUrl == null || baseUrl.isEmpty()) {
            throw new IllegalArgumentException("baseUrl must not be empty");
        }
        return new Environment(baseUrl);
    }

    public String baseUrl() {
        return baseUrl;
    }
}

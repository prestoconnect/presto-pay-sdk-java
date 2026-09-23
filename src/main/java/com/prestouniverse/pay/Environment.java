package com.prestouniverse.pay;

/**
 * Presto Connect gateway endpoints. Use {@code PrestoPayClient.Builder.baseUrl(...)} for any other host.
 */
public final class Environment {

    public static final Environment STAGING = new Environment("STAGING", "https://presto-stg-ext.enovax.com");
    public static final Environment PRODUCTION = new Environment("PRODUCTION", "https://pay-ext.prestouniverse.com");

    private final String name;
    private final String baseUrl;

    private Environment(String name, String baseUrl) {
        this.name = name;
        this.baseUrl = baseUrl;
    }

    public String baseUrl() {
        return baseUrl;
    }

    @Override
    public String toString() {
        return name + " (" + baseUrl + ")";
    }
}

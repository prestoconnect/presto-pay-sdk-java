package com.prestouniverse.pay.sample.transport;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * {@link HttpTransport} backed by the JDK 11+ {@link HttpClient}, which pools connections and speaks
 * HTTP/2 where the server supports it, unlike the SDK's default {@code HttpURLConnection}-based
 * transport ({@link HttpTransport#jdkDefault()}).
 *
 * <p>Timeouts are fixed at construction. {@code PrestoPayClient.Builder.connectTimeout()} and
 * {@code .readTimeout()} are ignored when this transport is supplied; configure timeouts through the
 * constructor instead.
 */
public final class JdkHttpClientTransport implements HttpTransport {

    private final HttpClient client;
    private final Duration readTimeout;

    public JdkHttpClientTransport() {
        this(Duration.ofSeconds(5), Duration.ofSeconds(30));
    }

    public JdkHttpClientTransport(Duration connectTimeout, Duration readTimeout) {
        this.client = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        this.readTimeout = readTimeout;
    }

    @Override
    public HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout) {
        java.net.http.HttpRequest.Builder builder = java.net.http.HttpRequest.newBuilder(URI.create(request.url()))
                .timeout(this.readTimeout)
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString(request.body(), StandardCharsets.UTF_8));
        request.headers().forEach(builder::header);

        try {
            java.net.http.HttpResponse<String> response = client.send(builder.build(),
                    java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            return HttpResponse.of(response.statusCode(), flatten(response.headers().map()), response.body());
        } catch (ConnectException | UnknownHostException e) {
            // The JDK HttpClient throws these only when the TCP handshake itself failed, so no bytes
            // of our request could have reached the gateway yet.
            throw new PrestoPayTransportException("Failed to connect to " + request.url(), e, true);
        } catch (IOException e) {
            throw new PrestoPayTransportException("HTTP call to " + request.url() + " failed", e, false);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PrestoPayTransportException("Interrupted calling " + request.url(), e, false);
        }
    }

    private static Map<String, String> flatten(Map<String, List<String>> headers) {
        Map<String, String> flat = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                flat.put(name, values.get(0));
            }
        });
        return flat;
    }
}

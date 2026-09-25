package com.prestouniverse.pay.sample.transport;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link HttpTransport} backed by Spring's {@link RestClient} (Spring Framework 6.1+ / Spring Boot 3.2+),
 * for integrators who already standardize HTTP calls on RestClient elsewhere (interceptors, Micrometer
 * observations, request/response logging).
 *
 * <p>Timeouts are fixed at construction. {@code PrestoPayClient.Builder.connectTimeout()} and
 * {@code .readTimeout()} are ignored when this transport is supplied; configure timeouts through the
 * constructor instead.
 *
 * <p>Uses {@link RestClient.RequestHeadersSpec#exchange} rather than {@code retrieve()} so that non-2xx
 * responses are returned to the SDK instead of thrown, per the {@link HttpTransport} contract.
 */
public final class RestClientHttpTransport implements HttpTransport {

    private final RestClient restClient;

    public RestClientHttpTransport() {
        this(Duration.ofSeconds(5), Duration.ofSeconds(30));
    }

    public RestClientHttpTransport(Duration connectTimeout, Duration readTimeout) {
        HttpClient jdkClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(jdkClient);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout) {
        try {
            return restClient.post()
                    .uri(request.url())
                    .headers(headers -> request.headers().forEach(headers::add))
                    .body(request.body())
                    .exchange((clientRequest, clientResponse) -> {
                        String body;
                        try (InputStream in = clientResponse.getBody()) {
                            body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                        }
                        return HttpResponse.of(
                                clientResponse.getStatusCode().value(),
                                flatten(clientResponse.getHeaders()),
                                body);
                    });
        } catch (ResourceAccessException e) {
            // RestClient wraps every I/O failure, including a failed connect, in ResourceAccessException;
            // only a connect-phase failure means the request certainly never left this process.
            Throwable cause = e.getCause();
            boolean requestNotSent = cause instanceof ConnectException || cause instanceof UnknownHostException;
            throw new PrestoPayTransportException("HTTP call to " + request.url() + " failed", e, requestNotSent);
        }
    }

    private static Map<String, String> flatten(HttpHeaders headers) {
        Map<String, String> flat = new LinkedHashMap<>();
        headers.forEach((name, values) -> {
            if (!values.isEmpty()) {
                flat.put(name, values.get(0));
            }
        });
        return flat;
    }
}

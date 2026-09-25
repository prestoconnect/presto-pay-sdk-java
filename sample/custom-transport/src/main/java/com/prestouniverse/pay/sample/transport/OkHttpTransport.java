package com.prestouniverse.pay.sample.transport;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;

import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * {@link HttpTransport} backed by OkHttp, for integrators who already depend on it elsewhere
 * (interceptors, connection pool metrics, HTTP/2).
 *
 * <p>Timeouts are fixed at construction. {@code PrestoPayClient.Builder.connectTimeout()} and
 * {@code .readTimeout()} are ignored when this transport is supplied; configure timeouts through the
 * constructor instead.
 *
 * <p>OkHttp's automatic retry-on-connection-failure is disabled: the SDK's idempotency guarantees for
 * {@code init}, {@code reverse}, and {@code refund} depend on the transport never resending a request
 * on its own.
 */
public final class OkHttpTransport implements HttpTransport {

    private static final MediaType JSON = MediaType.parse("application/json; charset=UTF-8");

    private final OkHttpClient client;

    public OkHttpTransport() {
        this(Duration.ofSeconds(5), Duration.ofSeconds(30));
    }

    public OkHttpTransport(Duration connectTimeout, Duration readTimeout) {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(connectTimeout)
                .readTimeout(readTimeout)
                .retryOnConnectionFailure(false)
                .build();
    }

    @Override
    public HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout) {
        RequestBody body = RequestBody.create(request.body(), JSON);
        okhttp3.Request.Builder builder = new okhttp3.Request.Builder()
                .url(request.url())
                .post(body);
        request.headers().forEach(builder::header);

        try (okhttp3.Response response = client.newCall(builder.build()).execute()) {
            ResponseBody responseBody = response.body();
            String bodyString = responseBody != null ? responseBody.string() : "";
            return HttpResponse.of(response.code(), flatten(response.headers()), bodyString);
        } catch (ConnectException | UnknownHostException e) {
            throw new PrestoPayTransportException("Failed to connect to " + request.url(), e, true);
        } catch (IOException e) {
            throw new PrestoPayTransportException("HTTP call to " + request.url() + " failed", e, false);
        }
    }

    private static Map<String, String> flatten(Headers headers) {
        Map<String, String> flat = new LinkedHashMap<>();
        for (String name : headers.names()) {
            flat.put(name, headers.get(name));
        }
        return flat;
    }
}

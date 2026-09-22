package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JdkHttpTransport implements HttpTransport {

    @Override
    public HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout) {
        HttpURLConnection connection = null;
        boolean requestSent = false;
        try {
            URL url = new URL(request.url());
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout((int) connectTimeout.toMillis());
            connection.setReadTimeout((int) readTimeout.toMillis());
            for (Map.Entry<String, String> header : request.headers().entrySet()) {
                connection.setRequestProperty(header.getKey(), header.getValue());
            }

            byte[] bodyBytes = request.body().getBytes(StandardCharsets.UTF_8);
            try (OutputStream out = connection.getOutputStream()) {
                out.write(bodyBytes);
            }
            requestSent = true;

            int status = connection.getResponseCode();
            InputStream stream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            String responseBody = readBody(stream);
            Map<String, String> headers = flattenHeaders(connection.getHeaderFields());
            return HttpResponse.of(status, headers, responseBody);
        } catch (IOException e) {
            throw new PrestoPayTransportException("HTTP call to " + request.url() + " failed", e, !requestSent);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static String readBody(InputStream in) throws IOException {
        if (in == null) {
            return "";
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read = in.read(chunk);
        while (read != -1) {
            buffer.write(chunk, 0, read);
            read = in.read(chunk);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    private static Map<String, String> flattenHeaders(Map<String, List<String>> raw) {
        Map<String, String> flattened = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
            if (entry.getKey() == null || entry.getValue().isEmpty()) {
                continue;
            }
            flattened.put(entry.getKey(), entry.getValue().get(0));
        }
        return flattened;
    }
}

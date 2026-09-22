package com.prestouniverse.pay.support;

import com.prestouniverse.pay.crypto.Canonicalizer;
import com.prestouniverse.pay.crypto.RsaSignatureService;
import com.prestouniverse.pay.internal.JsonCodec;
import com.prestouniverse.pay.internal.json.JsonObject;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MockGatewayServer implements AutoCloseable {

    private final HttpServer server;
    private volatile Handler handler;
    private volatile JsonObject lastRequestBody;
    private volatile Map<String, String> lastRequestHeaders;

    private MockGatewayServer(HttpServer server) {
        this.server = server;
    }

    public static MockGatewayServer start() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            MockGatewayServer mock = new MockGatewayServer(server);
            server.createContext("/", mock::dispatch);
            server.start();
            return mock;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to start mock gateway server", e);
        }
    }

    public void handler(Handler handler) {
        this.handler = handler;
    }

    public String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public JsonObject lastRequestBody() {
        return lastRequestBody;
    }

    public Map<String, String> lastRequestHeaders() {
        return lastRequestHeaders;
    }

    private void dispatch(HttpExchange exchange) throws IOException {
        String rawBody = readAll(exchange.getRequestBody());
        lastRequestBody = JsonCodec.parseObject(rawBody);
        lastRequestHeaders = flatten(exchange.getRequestHeaders());

        Response response = handler.handle(lastRequestBody);
        for (Map.Entry<String, String> header : response.headers.entrySet()) {
            exchange.getResponseHeaders().add(header.getKey(), header.getValue());
        }
        byte[] bytes = response.body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(response.status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
        exchange.getResponseBody().close();
    }

    private static Map<String, String> flatten(Headers headers) {
        Map<String, String> flattened = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                flattened.put(entry.getKey(), entry.getValue().get(0));
            }
        }
        return flattened;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int read = in.read(chunk);
        while (read != -1) {
            buffer.write(chunk, 0, read);
            read = in.read(chunk);
        }
        return new String(buffer.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        server.stop(0);
    }

    @FunctionalInterface
    public interface Handler {
        Response handle(JsonObject requestBody);
    }

    public static final class Response {

        private final int status;
        private final Map<String, String> headers;
        private final String body;

        private Response(int status, Map<String, String> headers, String body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        public static Response signed(int status, JsonObject body) {
            String canonical = Canonicalizer.canonicalize(body);
            String signature = RsaSignatureService.sign(canonical, TestKeys.privateKey());
            body.put("signature", signature);
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");
            return new Response(status, headers, JsonCodec.write(body));
        }

        public static Response preSigned(int status, JsonObject body) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("Content-Type", "application/json; charset=UTF-8");
            return new Response(status, headers, JsonCodec.write(body));
        }

        public static Response systemError(int status, String errorCode, String errorMessage) {
            Map<String, String> headers = new LinkedHashMap<>();
            headers.put("x-http-error-code", errorCode);
            headers.put("x-http-error", errorMessage);
            return new Response(status, headers, "");
        }
    }
}

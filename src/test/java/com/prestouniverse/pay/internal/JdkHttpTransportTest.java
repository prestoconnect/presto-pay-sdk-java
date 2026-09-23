package com.prestouniverse.pay.internal;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.support.MockGatewayServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdkHttpTransportTest {

    private static final String BODY = "{\"displayDesc\":\"訂單 😀\"}";

    private final JdkHttpTransport transport = new JdkHttpTransport();

    @Test
    void connectionRefusedIsReportedAsNotSent() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }

        PrestoPayTransportException exception = assertThrows(PrestoPayTransportException.class,
                () -> transport.execute(post("http://127.0.0.1:" + closedPort + "/v1/ext/payment/init"),
                        Duration.ofSeconds(2), Duration.ofSeconds(2)));

        assertTrue(exception.requestNotSent());
    }

    @Test
    void readTimeoutAfterSendIsReportedAsPossiblySent() {
        try (MockGatewayServer mockGateway = MockGatewayServer.start()) {
            mockGateway.handler(request -> {
                sleep(1000);
                return MockGatewayServer.Response.raw(200, "{}");
            });

            PrestoPayTransportException exception = assertThrows(PrestoPayTransportException.class,
                    () -> transport.execute(post(mockGateway.baseUrl() + "/v1/ext/payment/init"),
                            Duration.ofSeconds(2), Duration.ofMillis(200)));

            assertFalse(exception.requestNotSent());
        }
    }

    @Test
    void postIsNotSilentlyResentWhenAKeepAliveConnectionDropsAfterReceivingIt() throws Exception {
        try (DroppingServer server = new DroppingServer()) {
            String url = "http://127.0.0.1:" + server.port() + "/v1/ext/payment/init";
            transport.execute(post(url), Duration.ofSeconds(2), Duration.ofSeconds(2));

            PrestoPayTransportException exception = assertThrows(PrestoPayTransportException.class,
                    () -> transport.execute(post(url), Duration.ofSeconds(2), Duration.ofSeconds(2)));

            assertFalse(exception.requestNotSent());
            assertEquals(2, server.requestCount());
        }
    }

    @Test
    void redirectsAreNotFollowed() {
        try (MockGatewayServer mockGateway = MockGatewayServer.start()) {
            mockGateway.handler(request -> MockGatewayServer.Response.redirect(mockGateway.baseUrl() + "/elsewhere"));

            HttpResponse response = transport.execute(post(mockGateway.baseUrl() + "/v1/ext/payment/init"),
                    Duration.ofSeconds(2), Duration.ofSeconds(2));

            assertEquals(302, response.status());
        }
    }

    private static HttpRequest post(String url) {
        return HttpRequest.post(url, Collections.singletonMap("Content-Type", "application/json"), BODY);
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Answers the first request with keep-alive, then reads and drops every later request unanswered. */
    private static final class DroppingServer implements AutoCloseable {

        private final ServerSocket serverSocket;
        private final AtomicInteger requestCount = new AtomicInteger();
        private final Thread acceptor;

        DroppingServer() throws IOException {
            serverSocket = new ServerSocket(0, 50, InetAddress.getLoopbackAddress());
            acceptor = new Thread(this::acceptLoop, "dropping-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }

        int port() {
            return serverSocket.getLocalPort();
        }

        int requestCount() {
            return requestCount.get();
        }

        private void acceptLoop() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    serve(socket);
                } catch (IOException e) {
                    // expected when the client or test closes the connection
                }
            }
        }

        private void serve(Socket socket) throws IOException {
            InputStream in = socket.getInputStream();
            OutputStream out = socket.getOutputStream();
            while (readRequest(in)) {
                if (requestCount.incrementAndGet() > 1) {
                    return;
                }
                out.write(("HTTP/1.1 200 OK\r\nContent-Type: application/json\r\nContent-Length: 2\r\n"
                        + "Connection: keep-alive\r\n\r\n{}").getBytes(StandardCharsets.US_ASCII));
                out.flush();
            }
        }

        private static boolean readRequest(InputStream in) throws IOException {
            StringBuilder head = new StringBuilder();
            while (head.indexOf("\r\n\r\n") < 0) {
                int b = in.read();
                if (b == -1) {
                    return false;
                }
                head.append((char) b);
            }
            int contentLength = 0;
            for (String line : head.toString().split("\r\n")) {
                if (line.toLowerCase(Locale.ROOT).startsWith("content-length:")) {
                    contentLength = Integer.parseInt(line.substring("content-length:".length()).trim());
                }
            }
            for (int i = 0; i < contentLength; i++) {
                if (in.read() == -1) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public void close() throws IOException {
            serverSocket.close();
        }
    }
}

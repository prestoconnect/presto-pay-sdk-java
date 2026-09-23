package com.prestouniverse.pay.http;

import com.prestouniverse.pay.internal.JdkHttpTransport;

import java.time.Duration;

/**
 * Sends signed gateway requests. Implement it to add proxies, connection pooling, or observability, and pass it
 * to {@code PrestoPayClient.Builder.transport(...)}. To decorate the default behaviour, wrap
 * {@link #jdkDefault()}.
 *
 * <p>Implementations must be thread-safe and must follow this contract, which the SDK's retry and idempotency
 * guarantees depend on:
 * <ul>
 *   <li>Return every HTTP response, including non-2xx statuses, as an {@link HttpResponse}. Do not follow
 *       redirects.</li>
 *   <li>Throw {@link com.prestouniverse.pay.exception.PrestoPayTransportException} for I/O failures, with
 *       {@code requestNotSent = true} only when the request certainly never left this process (for example,
 *       connection refused or DNS failure). Once any byte may have been sent, report {@code false}.</li>
 *   <li>Never resend a request on your own; {@code init}, {@code reverse}, and {@code refund} are not
 *       idempotent.</li>
 * </ul>
 */
public interface HttpTransport {

    /**
     * Returns the built-in transport based on {@link java.net.HttpURLConnection}.
     */
    static HttpTransport jdkDefault() {
        return new JdkHttpTransport();
    }

    /**
     * Executes one HTTP POST.
     *
     * @param request the signed request; its body is UTF-8 JSON
     * @param connectTimeout maximum time to establish the connection
     * @param readTimeout maximum time to wait for response data
     * @return the response, whatever its status code
     */
    HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout);
}

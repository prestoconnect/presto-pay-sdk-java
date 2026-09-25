# Custom `HttpTransport` reference

Three reference implementations of `com.prestouniverse.pay.http.HttpTransport`, for integrators who
want to reuse an HTTP client they already depend on elsewhere instead of the SDK's default
`HttpURLConnection`-based transport (`HttpTransport.jdkDefault()`):

| Class | Backed by | Why you might pick it |
|-------|-----------|------------------------|
| `RestClientHttpTransport` | Spring's [`RestClient`](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html) (Spring Framework 6.1+ / Spring Boot 3.2+) | Already standardized on RestClient for interceptors, Micrometer observations, or request/response logging |
| `JdkHttpClientTransport` | `java.net.http.HttpClient` (JDK 11+) | Connection pooling and HTTP/2 with no extra dependency beyond the JDK |
| `OkHttpTransport` | [OkHttp](https://square.github.io/okhttp/) | Already depend on OkHttp elsewhere (interceptors, connection pool metrics) |

This module is **reference code to copy from, not a library to depend on** — copy the class you need
into your own codebase and adapt it (for example, to reuse a `RestClient`/`OkHttpClient` bean your
application already manages). It targets Java 17 because Spring's `RestClient` needs Spring Framework
6.1+; the SDK itself stays on Java 8.

## Contract these implementations follow

Every `HttpTransport` implementation must, per its Javadoc:

- Return every response — including non-2xx statuses — as an `HttpResponse`, never throw for a
  gateway-level HTTP error. All three classes here use each client's non-throwing API
  (`RestClient`'s `exchange(...)` rather than `retrieve()`, plain `HttpClient.send(...)`, and OkHttp's
  `Call.execute()`) so error responses come back normally.
- Never resend a request on its own — `init`, `reverse`, and `refund` are not idempotent. OkHttp
  retries on connection failure by default, so `OkHttpTransport` disables that
  (`retryOnConnectionFailure(false)`).
- Throw `PrestoPayTransportException` with `requestNotSent(true)` only when the request certainly
  never left the process (connection refused, DNS failure) — each implementation classifies the
  underlying client's connect-phase exceptions (`java.net.ConnectException`,
  `java.net.UnknownHostException`) this way and treats everything else as ambiguous
  (`requestNotSent(false)`).

## Timeouts

All three fix `connectTimeout`/`readTimeout` at construction rather than honoring the per-call values
`PrestoPayClient` passes to `HttpTransport.execute(...)`. That keeps each one's underlying client (and
its connection pool) built once instead of rebuilt per call. If you copy one of these, pass your own
timeouts to the constructor — `PrestoPayClient.Builder.connectTimeout()` / `.readTimeout()` have no
effect once a custom transport is supplied.

## Wiring one in

```java
PrestoPayClient client = PrestoPayClient.builder()
        .environment(Environment.STAGING)
        .merchantId(mid)
        .privateKey(privateKey)
        .prestoPublicKey(prestoPublicKey)
        .transport(new RestClientHttpTransport())
        .build();
```

`CustomTransportDemoApp` shows this end to end: it builds a client from the same `PRESTOPAY_*`
environment variables as `PrestoPayClient.fromEnv()` (see the main [README](../../README.md)), picks a
transport via `PRESTOPAY_TRANSPORT` (`rest-client`, `jdk-http-client`, or `okhttp`), and issues one
`query` call against `PRESTOPAY_QUERY_TXN_REF_NUM`.

```bash
cd sample/custom-transport-demo
mvn compile dependency:build-classpath -Dmdep.outputFile=target/cp.txt

export PRESTOPAY_ENV=staging
export PRESTOPAY_MID=...
export PRESTOPAY_MRN=...
export PRESTOPAY_KEYSTORE_PATH=...
export PRESTOPAY_KEYSTORE_PASSWORD=...
export PRESTOPAY_PUBLIC_KEY_PATH=...
export PRESTOPAY_QUERY_TXN_REF_NUM=...
export PRESTOPAY_TRANSPORT=rest-client   # or jdk-http-client, okhttp

java -cp "target/classes:$(cat target/cp.txt)" \
    com.prestouniverse.pay.sample.transport.CustomTransportDemoApp
```

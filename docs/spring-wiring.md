# Spring Boot wiring (minimal)

Register a singleton `PrestoPayClient` from configuration properties or environment variables.

```java
@Configuration
public class PrestoPayConfiguration {

    @Bean(destroyMethod = "") // client holds no resources to close
    PrestoPayClient prestoPayClient() {
        return PrestoPayClient.fromEnv();
    }
}
```

In a controller, inject the client and delegate to `client.payments()` / `client.webhooks()`.

For webhook endpoints, verify the raw body before parsing JSON with your web framework:

```java
@PostMapping("/presto/notify")
ResponseEntity<String> notify(@RequestBody String body) {
    NotifyEvent event = prestoPayClient.webhooks().parse(body);
    String suggestedStatus = event.getPaymentStatus();
    // enqueue work asynchronously, then:
    return ResponseEntity.ok(NotifyAck.ok());
}
```

Use `WebhookVerifier.builder()` instead if the service never calls the gateway API and only verifies callbacks.

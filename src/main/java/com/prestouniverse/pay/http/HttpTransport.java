package com.prestouniverse.pay.http;

import java.time.Duration;

public interface HttpTransport {

    HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout);
}

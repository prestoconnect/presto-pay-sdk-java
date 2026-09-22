package com.prestouniverse.pay.support;

import com.prestouniverse.pay.exception.PrestoPayTransportException;
import com.prestouniverse.pay.http.HttpRequest;
import com.prestouniverse.pay.http.HttpResponse;
import com.prestouniverse.pay.http.HttpTransport;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public final class FlakyTransport implements HttpTransport {

    private final HttpTransport delegate;
    private final int failuresBeforeSuccess;
    private final boolean requestNotSent;
    private final AtomicInteger callCount = new AtomicInteger();

    public FlakyTransport(HttpTransport delegate, int failuresBeforeSuccess, boolean requestNotSent) {
        this.delegate = delegate;
        this.failuresBeforeSuccess = failuresBeforeSuccess;
        this.requestNotSent = requestNotSent;
    }

    @Override
    public HttpResponse execute(HttpRequest request, Duration connectTimeout, Duration readTimeout) {
        int call = callCount.incrementAndGet();
        if (call <= failuresBeforeSuccess) {
            throw new PrestoPayTransportException("Simulated transport failure", null, requestNotSent);
        }
        return delegate.execute(request, connectTimeout, readTimeout);
    }

    public int callCount() {
        return callCount.get();
    }
}

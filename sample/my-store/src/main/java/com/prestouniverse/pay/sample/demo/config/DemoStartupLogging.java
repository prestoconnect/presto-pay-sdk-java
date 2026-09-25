package com.prestouniverse.pay.sample.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DemoStartupLogging implements ApplicationListener<ApplicationReadyEvent> {

    private static final Logger log = LoggerFactory.getLogger(DemoStartupLogging.class);

    private final AppProperties appProperties;
    private final PrestoPayProperties prestoPayProperties;

    @Value("${server.port:8080}")
    private int serverPort;

    public DemoStartupLogging(AppProperties appProperties, PrestoPayProperties prestoPayProperties) {
        this.appProperties = appProperties;
        this.prestoPayProperties = prestoPayProperties;
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("Presto Pay demo ready on port {}", serverPort);
        log.info("Checkout: http://localhost:{}/", serverPort);
        log.info("Gateway: env={} mid={} prestoMrn={}",
                prestoPayProperties.getEnvironment(),
                prestoPayProperties.getMid(),
                prestoPayProperties.getMrn());
        log.info("Callbacks (must be reachable by Presto): notifyUrl={} redirectUrl={}",
                appProperties.notifyUrl(), appProperties.redirectUrl());
    }
}

package com.prestouniverse.pay.sample.demo.service.support;

import java.util.UUID;

public final class DemoTxnReferenceGenerator {

    private static final String PREFIX = "demo-";

    private DemoTxnReferenceGenerator() {
    }

    public static String next() {
        return PREFIX + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}

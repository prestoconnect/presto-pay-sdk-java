package com.prestouniverse.pay.internal;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class Timestamps {

    public static final ZoneOffset GATEWAY_ZONE = ZoneOffset.of("+08:00");

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS").withZone(GATEWAY_ZONE);

    private Timestamps() {
    }

    public static String format(Instant instant) {
        return FORMATTER.format(instant);
    }

    public static String now(Clock clock) {
        return format(clock.instant());
    }

    public static Instant parse(String value) {
        return LocalDateTime.parse(value, FORMATTER).atZone(GATEWAY_ZONE).toInstant();
    }
}

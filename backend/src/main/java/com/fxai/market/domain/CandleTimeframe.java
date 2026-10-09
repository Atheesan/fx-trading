package com.fxai.market.domain;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

public enum CandleTimeframe {
    ONE_MINUTE("1m", Duration.ofMinutes(1)),
    FIVE_MINUTES("5m", Duration.ofMinutes(5)),
    FIFTEEN_MINUTES("15m", Duration.ofMinutes(15)),
    ONE_HOUR("1h", Duration.ofHours(1)),
    ONE_DAY("1d", Duration.ofDays(1));

    private final String code;
    private final Duration duration;

    CandleTimeframe(String code, Duration duration) {
        this.code = code;
        this.duration = duration;
    }

    public String code() { return code; }
    public Duration duration() { return duration; }

    public static Optional<CandleTimeframe> fromCode(String code) {
        return code == null ? Optional.empty() : Arrays.stream(values())
                .filter(value -> value.code.equalsIgnoreCase(code))
                .findFirst();
    }
}

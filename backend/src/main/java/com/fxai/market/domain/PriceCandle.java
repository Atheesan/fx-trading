package com.fxai.market.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PriceCandle(String symbol, CandleTimeframe timeframe, BigDecimal open,
        BigDecimal high, BigDecimal low, BigDecimal close, long volume,
        boolean complete, Instant timestamp) {

    public PriceCandle {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(timeframe, "timeframe must not be null");
        Objects.requireNonNull(open, "open must not be null");
        Objects.requireNonNull(high, "high must not be null");
        Objects.requireNonNull(low, "low must not be null");
        Objects.requireNonNull(close, "close must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        if (symbol.isBlank()) throw new IllegalArgumentException("symbol must not be blank");
        if (volume < 0) throw new IllegalArgumentException("volume must not be negative");
        if (high.compareTo(open) < 0 || high.compareTo(close) < 0 || high.compareTo(low) < 0)
            throw new IllegalArgumentException("high must be the greatest candle price");
        if (low.compareTo(open) > 0 || low.compareTo(close) > 0)
            throw new IllegalArgumentException("low must be the lowest candle price");
    }
}

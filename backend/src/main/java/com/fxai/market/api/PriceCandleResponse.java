package com.fxai.market.api;

import com.fxai.market.domain.PriceCandle;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceCandleResponse(
        String symbol, String timeframe, BigDecimal open, BigDecimal high,
        BigDecimal low, BigDecimal close, long volume, boolean complete, Instant timestamp) {
    public static PriceCandleResponse from(PriceCandle candle) {
        return new PriceCandleResponse(candle.symbol(), candle.timeframe().code(), candle.open(), candle.high(),
                candle.low(), candle.close(), candle.volume(), candle.complete(), candle.timestamp());
    }
}

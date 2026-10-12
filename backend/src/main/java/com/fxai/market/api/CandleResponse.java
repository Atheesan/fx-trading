package com.fxai.market.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fxai.market.domain.Candle;
import java.math.BigDecimal;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CandleResponse(
		String symbol,
		String timeframe,
		Instant timestamp,
		BigDecimal open,
		BigDecimal high,
		BigDecimal low,
		BigDecimal close,
		Long volume
) {

	public static CandleResponse from(Candle candle) {
		return new CandleResponse(
				candle.symbol(),
				candle.timeframe().code(),
				candle.timestamp(),
				candle.open(),
				candle.high(),
				candle.low(),
				candle.close(),
				candle.volume()
		);
	}

}

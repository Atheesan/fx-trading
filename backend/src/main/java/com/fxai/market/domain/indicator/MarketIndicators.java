package com.fxai.market.domain.indicator;

import com.fxai.market.domain.CandleTimeframe;

import java.util.List;
import java.util.Objects;

public record MarketIndicators(
		String symbol,
		CandleTimeframe timeframe,
		List<IndicatorResult> indicators
) {
	public MarketIndicators {
		Objects.requireNonNull(symbol, "symbol must not be null");
		Objects.requireNonNull(timeframe, "timeframe must not be null");
		indicators = List.copyOf(indicators);
	}
}

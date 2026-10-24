package com.fxai.market.api;

import com.fxai.market.domain.indicator.MarketIndicators;

import java.util.List;

public record MarketIndicatorsResponse(
		String symbol,
		String timeframe,
		List<IndicatorResponse> indicators
) {
	static MarketIndicatorsResponse from(MarketIndicators indicators) {
		return new MarketIndicatorsResponse(
				indicators.symbol(),
				indicators.timeframe().code(),
				indicators.indicators().stream().map(IndicatorResponse::from).toList()
		);
	}
}

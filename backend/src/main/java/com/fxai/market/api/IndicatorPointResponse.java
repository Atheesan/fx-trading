package com.fxai.market.api;

import com.fxai.market.domain.indicator.IndicatorPoint;

import java.math.BigDecimal;
import java.time.Instant;

public record IndicatorPointResponse(
		Instant timestamp,
		BigDecimal value,
		BigDecimal signal,
		BigDecimal histogram
) {
	static IndicatorPointResponse from(IndicatorPoint point) {
		return new IndicatorPointResponse(
				point.timestamp(),
				point.value(),
				point.signal(),
				point.histogram()
		);
	}
}

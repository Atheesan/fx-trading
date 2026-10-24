package com.fxai.market.domain.indicator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record IndicatorPoint(
		Instant timestamp,
		BigDecimal value,
		BigDecimal signal,
		BigDecimal histogram
) {
	public IndicatorPoint {
		Objects.requireNonNull(timestamp, "timestamp must not be null");
		Objects.requireNonNull(value, "value must not be null");
	}

	public static IndicatorPoint value(Instant timestamp, BigDecimal value) {
		return new IndicatorPoint(timestamp, value, null, null);
	}

	public static IndicatorPoint macd(
			Instant timestamp,
			BigDecimal macd,
			BigDecimal signal,
			BigDecimal histogram
	) {
		Objects.requireNonNull(signal, "signal must not be null");
		Objects.requireNonNull(histogram, "histogram must not be null");
		return new IndicatorPoint(timestamp, macd, signal, histogram);
	}
}

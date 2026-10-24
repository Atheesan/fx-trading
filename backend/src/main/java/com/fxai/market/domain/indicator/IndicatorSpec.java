package com.fxai.market.domain.indicator;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public enum IndicatorSpec {
	SMA_20("sma20", IndicatorType.SMA, 20, 20),
	SMA_50("sma50", IndicatorType.SMA, 50, 50),
	EMA_20("ema20", IndicatorType.EMA, 20, 20),
	RSI_14("rsi14", IndicatorType.RSI, 14, 15),
	MACD("macd", IndicatorType.MACD, null, 34);

	private final String requestName;
	private final IndicatorType type;
	private final Integer period;
	private final int requiredDataPoints;

	IndicatorSpec(String requestName, IndicatorType type, Integer period, int requiredDataPoints) {
		this.requestName = requestName;
		this.type = type;
		this.period = period;
		this.requiredDataPoints = requiredDataPoints;
	}

	public String requestName() {
		return requestName;
	}

	public IndicatorType type() {
		return type;
	}

	public Integer period() {
		return period;
	}

	public int requiredDataPoints() {
		return requiredDataPoints;
	}

	public static List<IndicatorSpec> parseRequested(String requested) {
		if (requested == null || requested.isBlank()) {
			throw new IllegalArgumentException("indicators must contain at least one supported indicator");
		}

		var parsed = new LinkedHashSet<IndicatorSpec>();
		for (String value : requested.split(",")) {
			String normalized = value.trim().toLowerCase(Locale.ROOT);
			IndicatorSpec indicator = Arrays.stream(values())
					.filter(candidate -> candidate.requestName.equals(normalized))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("Unsupported indicator: " + value.trim()));
			parsed.add(indicator);
		}
		return List.copyOf(parsed);
	}
}

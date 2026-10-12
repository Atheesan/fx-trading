package com.fxai.market.domain;

import java.time.Instant;
import java.util.Objects;

public record CandleQuery(CandleTimeframe timeframe, Instant from, Instant to, int limit) {

	public static final int DEFAULT_LIMIT = 500;
	public static final int MAX_LIMIT = 5000;

	public CandleQuery {
		Objects.requireNonNull(timeframe, "timeframe must not be null");
		if (limit < 1 || limit > MAX_LIMIT) {
			throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
		}
		if (from != null && to != null && !from.isBefore(to)) {
			throw new IllegalArgumentException("from must be before to");
		}
	}

}

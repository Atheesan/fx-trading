package com.fxai.market;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

public record PriceQuote(
		String symbol,
		BigDecimal bid,
		BigDecimal ask,
		BigDecimal midpoint,
		BigDecimal spread,
		Instant timestamp
) {

	private static final BigDecimal TWO = BigDecimal.valueOf(2);

	public PriceQuote {
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		Objects.requireNonNull(bid, "bid must not be null");
		Objects.requireNonNull(ask, "ask must not be null");
		Objects.requireNonNull(midpoint, "midpoint must not be null");
		Objects.requireNonNull(spread, "spread must not be null");
		Objects.requireNonNull(timestamp, "timestamp must not be null");

		if (bid.signum() < 0 || ask.signum() < 0) {
			throw new IllegalArgumentException("prices must not be negative");
		}
		if (bid.compareTo(ask) > 0) {
			throw new IllegalArgumentException("bid must not be greater than ask");
		}
		if (midpoint.compareTo(calculateMidpoint(bid, ask)) != 0) {
			throw new IllegalArgumentException("midpoint must be the average of bid and ask");
		}
		if (spread.compareTo(ask.subtract(bid)) != 0) {
			throw new IllegalArgumentException("spread must equal ask minus bid");
		}
	}

	public static PriceQuote fromBidAndAsk(
			String symbol,
			BigDecimal bid,
			BigDecimal ask,
			Instant timestamp
	) {
		Objects.requireNonNull(bid, "bid must not be null");
		Objects.requireNonNull(ask, "ask must not be null");
		return new PriceQuote(
				symbol,
				bid,
				ask,
				calculateMidpoint(bid, ask),
				ask.subtract(bid),
				timestamp
		);
	}

	private static BigDecimal calculateMidpoint(BigDecimal bid, BigDecimal ask) {
		return bid.add(ask).divide(TWO);
	}

}

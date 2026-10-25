package com.fxai.market.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Embeddable
public class HistoricalCandleId implements Serializable {
	@Column(name = "currency_pair_symbol", length = 6, nullable = false)
	private String currencyPairSymbol;

	@Column(length = 4, nullable = false)
	private String timeframe;

	@Column(name = "candle_time", nullable = false)
	private Instant timestamp;

	protected HistoricalCandleId() {
	}

	HistoricalCandleId(String currencyPairSymbol, String timeframe, Instant timestamp) {
		this.currencyPairSymbol = currencyPairSymbol;
		this.timeframe = timeframe;
		this.timestamp = timestamp;
	}

	String timeframe() {
		return timeframe;
	}

	Instant timestamp() {
		return timestamp;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof HistoricalCandleId that)) {
			return false;
		}
		return Objects.equals(currencyPairSymbol, that.currencyPairSymbol)
				&& Objects.equals(timeframe, that.timeframe)
				&& Objects.equals(timestamp, that.timestamp);
	}

	@Override
	public int hashCode() {
		return Objects.hash(currencyPairSymbol, timeframe, timestamp);
	}
}

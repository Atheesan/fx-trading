package com.fxai.market.messaging;

import com.fxai.instrument.SupportedCurrencyPairs;
import com.fxai.market.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Versioned wire contract, independent of provider payloads and public API DTOs. */
public record PriceTickEvent(UUID eventId, String eventType, int schemaVersion, String symbol,
		BigDecimal bid, BigDecimal ask, BigDecimal mid, BigDecimal spread, Instant timestamp, String source) {
	public static final String TOPIC = "market.price.tick.v1";
	public static final String TYPE = "MARKET_PRICE_TICK";

	public PriceTickEvent {
		Objects.requireNonNull(eventId, "eventId is required");
		if (!TYPE.equals(eventType) || schemaVersion != 1) {
			throw new IllegalArgumentException("Unsupported price tick event type or schema version");
		}
		if (SupportedCurrencyPairs.findBySymbol(symbol).filter(pair -> pair.symbol().equals(symbol)).isEmpty()) {
			throw new IllegalArgumentException("Unsupported price tick symbol");
		}
		if (source == null || source.isBlank()) {
			throw new IllegalArgumentException("source is required");
		}
		new PriceQuote(symbol, bid, ask, mid, spread, timestamp);
	}

	public static PriceTickEvent from(PriceQuote quote, String source) {
		return new PriceTickEvent(UUID.randomUUID(), TYPE, 1, quote.symbol(), quote.bid(), quote.ask(),
				quote.midpoint(), quote.spread(), quote.timestamp(), source);
	}

	public PriceQuote toQuote() {
		return new PriceQuote(symbol, bid, ask, mid, spread, timestamp);
	}
}

package com.fxai.market.api;

import com.fxai.market.PriceQuote;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceQuoteResponse(
		String symbol,
		BigDecimal bid,
		BigDecimal ask,
		BigDecimal midpoint,
		BigDecimal spread,
		Instant timestamp
) {

	static PriceQuoteResponse from(PriceQuote quote) {
		return new PriceQuoteResponse(
				quote.symbol(),
				quote.bid(),
				quote.ask(),
				quote.midpoint(),
				quote.spread(),
				quote.timestamp()
		);
	}

}

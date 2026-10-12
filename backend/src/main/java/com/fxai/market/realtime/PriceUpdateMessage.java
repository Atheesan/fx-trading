package com.fxai.market.realtime;

import com.fxai.market.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;

public record PriceUpdateMessage(
		String type,
		String symbol,
		BigDecimal bid,
		BigDecimal ask,
		BigDecimal mid,
		BigDecimal spread,
		Instant timestamp
) {

	private static final String PRICE_UPDATE = "PRICE_UPDATE";

	public static PriceUpdateMessage from(PriceQuote quote) {
		return new PriceUpdateMessage(
				PRICE_UPDATE,
				quote.symbol(),
				quote.bid(),
				quote.ask(),
				quote.midpoint(),
				quote.spread(),
				quote.timestamp()
		);
	}

}

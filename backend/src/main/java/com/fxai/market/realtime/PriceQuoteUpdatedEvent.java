package com.fxai.market.realtime;

import com.fxai.market.PriceQuote;
import java.util.Objects;

public record PriceQuoteUpdatedEvent(PriceQuote quote) {

	public PriceQuoteUpdatedEvent {
		Objects.requireNonNull(quote, "quote must not be null");
	}

}

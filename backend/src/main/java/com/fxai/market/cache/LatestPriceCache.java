package com.fxai.market.cache;

import com.fxai.market.PriceQuote;

import java.util.Optional;

public interface LatestPriceCache {
	Optional<PriceQuote> get(String symbol);

	void put(PriceQuote quote);
}

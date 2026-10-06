package com.fxai.market.provider;

import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;

import java.util.List;
import java.util.Optional;

public interface MarketDataProvider {

	List<CurrencyPair> getCurrencyPairs();

	Optional<PriceQuote> getPrice(String symbol);

}

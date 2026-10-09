package com.fxai.market.provider;

import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.domain.PriceCandle;

import java.util.List;
import java.util.Optional;

public interface MarketDataProvider {
	List<CurrencyPair> getCurrencyPairs();

	Optional<PriceQuote> getPrice(String symbol);

	Optional<List<PriceCandle>> getHistoricalCandles(String symbol, CandleTimeframe timeframe, int count);

}

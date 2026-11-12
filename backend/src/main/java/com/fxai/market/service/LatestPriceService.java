package com.fxai.market.service;

import com.fxai.instrument.SupportedCurrencyPairs;
import com.fxai.market.PriceQuote;
import com.fxai.market.cache.LatestPriceCache;
import com.fxai.market.provider.MarketDataProvider;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class LatestPriceService {
	private final MarketDataProvider marketDataProvider;
	private final LatestPriceCache latestPriceCache;

	public LatestPriceService(
			MarketDataProvider marketDataProvider,
			LatestPriceCache latestPriceCache
	) {
		this.marketDataProvider = marketDataProvider;
		this.latestPriceCache = latestPriceCache;
	}

	public Optional<PriceQuote> getLatestPrice(String symbol) {
		return SupportedCurrencyPairs.findBySymbol(symbol)
				.flatMap(pair -> latestPriceCache.get(pair.symbol())
						.or(() -> fetchAndCache(pair.symbol())));
	}

	private Optional<PriceQuote> fetchAndCache(String symbol) {
		Optional<PriceQuote> quote = marketDataProvider.getPrice(symbol);
		quote.ifPresent(latestPriceCache::put);
		return quote;
	}
}

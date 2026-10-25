package com.fxai.market.service;

import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.repository.HistoricalCandleRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HistoricalCandleService {
	private final MarketDataProvider marketDataProvider;
	private final HistoricalCandleRepository repository;

	public HistoricalCandleService(
			MarketDataProvider marketDataProvider,
			HistoricalCandleRepository repository
	) {
		this.marketDataProvider = marketDataProvider;
		this.repository = repository;
	}

	public Optional<List<Candle>> getHistoricalCandles(String symbol, CandleQuery query) {
		return marketDataProvider.getHistoricalCandles(symbol, query)
				.map(candles -> {
					repository.saveAll(candles);
					return candles;
				});
	}
}

package com.fxai.market.service;

import com.fxai.instrument.SupportedCurrencyPairs;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.domain.indicator.IndicatorSpec;
import com.fxai.market.domain.indicator.MarketIndicators;
import com.fxai.market.domain.indicator.TechnicalIndicatorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MarketIndicatorService {
	private final HistoricalCandleService historicalCandleService;
	private final TechnicalIndicatorService technicalIndicatorService;

	@Autowired
	public MarketIndicatorService(HistoricalCandleService historicalCandleService) {
		this(historicalCandleService, new TechnicalIndicatorService());
	}

	MarketIndicatorService(
			HistoricalCandleService historicalCandleService,
			TechnicalIndicatorService technicalIndicatorService
	) {
		this.historicalCandleService = historicalCandleService;
		this.technicalIndicatorService = technicalIndicatorService;
	}

	public Optional<MarketIndicators> calculate(
			String symbol,
			CandleTimeframe timeframe,
			List<IndicatorSpec> specifications
	) {
		return SupportedCurrencyPairs.findBySymbol(symbol).flatMap(pair -> {
			var query = new CandleQuery(timeframe, null, null, CandleQuery.DEFAULT_LIMIT);
			return historicalCandleService.getHistoricalCandles(pair.symbol(), query)
					.map(candles -> new MarketIndicators(
							pair.symbol(),
							timeframe,
							specifications.stream()
									.map(specification -> technicalIndicatorService.calculate(specification, candles))
									.toList()
					));
		});
	}
}

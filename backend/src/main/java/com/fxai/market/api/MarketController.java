package com.fxai.market.api;

import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.domain.indicator.IndicatorSpec;
import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.service.HistoricalCandleService;
import com.fxai.market.service.MarketIndicatorService;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
	private final MarketDataProvider marketDataProvider;
	private final HistoricalCandleService historicalCandleService;
	private final MarketIndicatorService marketIndicatorService;

	public MarketController(
			MarketDataProvider marketDataProvider,
			HistoricalCandleService historicalCandleService,
			MarketIndicatorService marketIndicatorService
	) {
		this.marketDataProvider = marketDataProvider;
		this.historicalCandleService = historicalCandleService;
		this.marketIndicatorService = marketIndicatorService;
	}

	@GetMapping("/pairs")
	public List<CurrencyPairResponse> getCurrencyPairs() {
		return marketDataProvider.getCurrencyPairs().stream()
				.map(CurrencyPairResponse::from)
				.toList();
	}

	@GetMapping("/prices/{symbol}")
	public PriceQuoteResponse getPrice(@PathVariable String symbol) {
		return marketDataProvider.getPrice(symbol)
				.map(PriceQuoteResponse::from)
				.orElseThrow(() -> new UnsupportedCurrencyPairException(symbol));
	}

	@GetMapping("/candles/{symbol}")
	public List<CandleResponse> getHistoricalCandles(
			@PathVariable String symbol,
			@RequestParam String timeframe,
			@RequestParam(required = false) String from,
			@RequestParam(required = false) String to,
			@RequestParam(defaultValue = "500") int limit
	) {
		CandleTimeframe parsedTimeframe = CandleTimeframe.fromCode(timeframe)
				.orElseThrow(() -> new InvalidMarketRequestException("Unsupported timeframe: " + timeframe));

		CandleQuery query;
		try {
			query = new CandleQuery(
					parsedTimeframe,
					parseInstant("from", from),
					parseInstant("to", to),
					limit
			);
		} catch (IllegalArgumentException exception) {
			throw new InvalidMarketRequestException(exception.getMessage());
		}

		return historicalCandleService.getHistoricalCandles(symbol, query)
				.orElseThrow(() -> new UnsupportedCurrencyPairException(symbol))
				.stream()
				.limit(query.limit())
				.map(CandleResponse::from)
				.toList();
	}

	@GetMapping("/indicators/{symbol}")
	public MarketIndicatorsResponse getIndicators(
			@PathVariable String symbol,
			@RequestParam String timeframe,
			@RequestParam String indicators
	) {
		CandleTimeframe parsedTimeframe = parseTimeframe(timeframe);
		List<IndicatorSpec> parsedIndicators;
		try {
			parsedIndicators = IndicatorSpec.parseRequested(indicators);
		} catch (IllegalArgumentException exception) {
			throw new InvalidMarketRequestException(exception.getMessage());
		}

		return marketIndicatorService.calculate(symbol, parsedTimeframe, parsedIndicators)
				.map(MarketIndicatorsResponse::from)
				.orElseThrow(() -> new UnsupportedCurrencyPairException(symbol));
	}

	private CandleTimeframe parseTimeframe(String timeframe) {
		return CandleTimeframe.fromCode(timeframe)
				.orElseThrow(() -> new InvalidMarketRequestException("Unsupported timeframe: " + timeframe));
	}

	private Instant parseInstant(String parameterName, String value) {
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException exception) {
			throw new IllegalArgumentException(parameterName + " must be an ISO-8601 UTC timestamp");
		}
	}

}

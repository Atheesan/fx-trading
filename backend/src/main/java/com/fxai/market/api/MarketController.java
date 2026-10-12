package com.fxai.market.api;

import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.provider.MarketDataProvider;
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

	public MarketController(MarketDataProvider marketDataProvider) {
		this.marketDataProvider = marketDataProvider;
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

		return marketDataProvider.getHistoricalCandles(symbol, query)
				.orElseThrow(() -> new UnsupportedCurrencyPairException(symbol))
				.stream()
				.limit(query.limit())
				.map(CandleResponse::from)
				.toList();
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

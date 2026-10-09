package com.fxai.market.api;

import com.fxai.market.provider.MarketDataProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/market")
public class MarketController {
    @org.springframework.web.bind.annotation.GetMapping("/candles/{symbol}")
    public java.util.List<PriceCandleResponse> getHistoricalCandles(
            @org.springframework.web.bind.annotation.PathVariable String symbol,
            @org.springframework.web.bind.annotation.RequestParam String timeframe,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "100") int count) {
        if (count < 1 || count > 5000) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "count must be between 1 and 5000");
        }
        com.fxai.market.domain.CandleTimeframe parsed = com.fxai.market.domain.CandleTimeframe.fromCode(timeframe)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Unsupported timeframe: " + timeframe));
        return marketDataProvider.getHistoricalCandles(symbol, parsed, count)
                .orElseThrow(() -> new UnsupportedCurrencyPairException(symbol))
                .stream().map(PriceCandleResponse::from).toList();
    }

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

}

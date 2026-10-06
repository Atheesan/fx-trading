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

package com.fxai.market.provider;

import com.fxai.market.PriceQuote;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class MockMarketDataProviderTests {

	private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");
	private final MockMarketDataProvider provider = new MockMarketDataProvider(
			Clock.fixed(NOW, ZoneOffset.UTC)
	);

	@Test
	void exposesAllInitiallySupportedPairs() {
		assertThat(provider.getCurrencyPairs())
				.extracting(pair -> pair.displayName())
				.containsExactly(
						"EUR/USD",
						"GBP/USD",
						"USD/JPY",
						"AUD/USD",
						"NZD/USD",
						"USD/CAD",
						"USD/CHF",
						"EUR/GBP"
				);
	}

	@Test
	void returnsAQuoteForASupportedSymbol() {
		PriceQuote quote = provider.getPrice("eur-usd").orElseThrow();

		assertThat(quote.symbol()).isEqualTo("EURUSD");
		assertThat(quote.bid()).isLessThan(quote.ask());
		assertThat(quote.timestamp()).isEqualTo(NOW);
	}

	@Test
	void returnsEmptyForAnUnsupportedSymbol() {
		assertThat(provider.getPrice("BTCUSD")).isEmpty();
	}

}

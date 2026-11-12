package com.fxai.market.service;

import com.fxai.market.PriceQuote;
import com.fxai.market.cache.LatestPriceCache;
import com.fxai.market.provider.MarketDataProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LatestPriceServiceTests {
	private static final PriceQuote QUOTE = PriceQuote.fromBidAndAsk(
			"EURUSD",
			new BigDecimal("1.08420"),
			new BigDecimal("1.08440"),
			Instant.parse("2026-01-02T03:04:05Z")
	);

	@Mock
	private MarketDataProvider marketDataProvider;

	@Mock
	private LatestPriceCache latestPriceCache;

	@InjectMocks
	private LatestPriceService service;

	@Test
	void returnsCachedQuoteWithoutCallingProvider() {
		when(latestPriceCache.get("EURUSD")).thenReturn(Optional.of(QUOTE));

		assertThat(service.getLatestPrice("eur-usd")).contains(QUOTE);

		verifyNoInteractions(marketDataProvider);
		verify(latestPriceCache, never()).put(QUOTE);
	}

	@Test
	void fetchesAndCachesProviderQuoteAfterCacheMiss() {
		when(latestPriceCache.get("EURUSD")).thenReturn(Optional.empty());
		when(marketDataProvider.getPrice("EURUSD")).thenReturn(Optional.of(QUOTE));

		assertThat(service.getLatestPrice("EURUSD")).contains(QUOTE);

		verify(latestPriceCache).put(QUOTE);
	}

	@Test
	void doesNotCacheUnsupportedSymbolsOrEmptyProviderResults() {
		assertThat(service.getLatestPrice("BTCUSD")).isEmpty();
		verifyNoInteractions(latestPriceCache, marketDataProvider);

		when(latestPriceCache.get("EURUSD")).thenReturn(Optional.empty());
		when(marketDataProvider.getPrice("EURUSD")).thenReturn(Optional.empty());

		assertThat(service.getLatestPrice("EURUSD")).isEmpty();
		verify(latestPriceCache, never()).put(QUOTE);
	}
}

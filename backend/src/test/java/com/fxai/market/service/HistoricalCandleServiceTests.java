package com.fxai.market.service;

import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.repository.HistoricalCandleRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HistoricalCandleServiceTests {
	private final MarketDataProvider provider = mock(MarketDataProvider.class);
	private final HistoricalCandleRepository repository = mock(HistoricalCandleRepository.class);
	private final HistoricalCandleService service = new HistoricalCandleService(provider, repository);

	@Test
	void persistsProviderCandlesBeforeReturningThem() {
		CandleQuery query = new CandleQuery(CandleTimeframe.ONE_HOUR, null, null, 10);
		BigDecimal price = new BigDecimal("1.10000");
		List<Candle> candles = List.of(new Candle(
				"EURUSD",
				CandleTimeframe.ONE_HOUR,
				Instant.parse("2026-05-01T00:00:00Z"),
				price,
				price,
				price,
				price,
				100L
		));
		when(provider.getHistoricalCandles("EURUSD", query)).thenReturn(Optional.of(candles));

		assertThat(service.getHistoricalCandles("EURUSD", query)).contains(candles);
		verify(repository).saveAll(candles);
	}
}

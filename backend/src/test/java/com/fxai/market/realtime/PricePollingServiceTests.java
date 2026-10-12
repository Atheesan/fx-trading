package com.fxai.market.realtime;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fxai.instrument.Currency;
import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;
import com.fxai.market.provider.MarketDataProvider;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PricePollingServiceTests {

	private static final CurrencyPair EUR_USD = new CurrencyPair(Currency.EUR, Currency.USD);
	private static final CurrencyPair GBP_USD = new CurrencyPair(Currency.GBP, Currency.USD);
	private static final Instant TIMESTAMP = Instant.parse("2026-01-02T03:04:05Z");

	@Mock
	private MarketDataProvider marketDataProvider;

	@Mock
	private ApplicationEventPublisher eventPublisher;

	private PricePollingService service;

	@BeforeEach
	void setUp() {
		service = new PricePollingService(marketDataProvider, eventPublisher);
		when(marketDataProvider.getCurrencyPairs()).thenReturn(List.of(EUR_USD, GBP_USD));
	}

	@Test
	void pollsEachPairOnceAndPublishesInternalEvents() {
		PriceQuote eurUsd = quote("EURUSD", "1.1000", "1.1002");
		PriceQuote gbpUsd = quote("GBPUSD", "1.2500", "1.2503");
		when(marketDataProvider.getPrice("EURUSD")).thenReturn(Optional.of(eurUsd));
		when(marketDataProvider.getPrice("GBPUSD")).thenReturn(Optional.of(gbpUsd));

		service.pollPrices();

		verify(marketDataProvider).getPrice("EURUSD");
		verify(marketDataProvider).getPrice("GBPUSD");
		ArgumentCaptor<PriceQuoteUpdatedEvent> events = ArgumentCaptor.forClass(PriceQuoteUpdatedEvent.class);
		verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(events.capture());
		org.assertj.core.api.Assertions.assertThat(events.getAllValues())
				.extracting(event -> event.quote().symbol())
				.containsExactly("EURUSD", "GBPUSD");
	}

	@Test
	void continuesPollingWhenOnePairFails() {
		doThrow(new IllegalStateException("provider unavailable"))
				.when(marketDataProvider).getPrice("EURUSD");
		PriceQuote gbpUsd = quote("GBPUSD", "1.2500", "1.2503");
		when(marketDataProvider.getPrice("GBPUSD")).thenReturn(Optional.of(gbpUsd));

		service.pollPrices();

		verify(marketDataProvider).getPrice("GBPUSD");
		verify(eventPublisher).publishEvent(new PriceQuoteUpdatedEvent(gbpUsd));
	}

	private PriceQuote quote(String symbol, String bid, String ask) {
		return PriceQuote.fromBidAndAsk(symbol, new BigDecimal(bid), new BigDecimal(ask), TIMESTAMP);
	}

}

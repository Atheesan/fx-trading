package com.fxai.market.api;

import com.fxai.instrument.Currency;
import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;
import com.fxai.market.provider.MarketDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MarketController.class)
class MarketControllerTests {

	private static final Instant TIMESTAMP = Instant.parse("2026-01-02T03:04:05Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MarketDataProvider marketDataProvider;

	@Test
	void returnsSupportedCurrencyPairs() throws Exception {
		when(marketDataProvider.getCurrencyPairs()).thenReturn(List.of(
				new CurrencyPair(Currency.EUR, Currency.USD),
				new CurrencyPair(Currency.GBP, Currency.USD)
		));

		mockMvc.perform(get("/api/v1/market/pairs"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$[0].baseCurrency").value("EUR"))
				.andExpect(jsonPath("$[0].quoteCurrency").value("USD"))
				.andExpect(jsonPath("$[0].symbol").value("EURUSD"))
				.andExpect(jsonPath("$[0].displayName").value("EUR/USD"))
				.andExpect(jsonPath("$[1].symbol").value("GBPUSD"));
	}

	@Test
	void returnsTheLatestPriceForASupportedSymbol() throws Exception {
		PriceQuote quote = PriceQuote.fromBidAndAsk(
				"EURUSD",
				new BigDecimal("1.08420"),
				new BigDecimal("1.08440"),
				TIMESTAMP
		);
		when(marketDataProvider.getPrice("EURUSD")).thenReturn(Optional.of(quote));

		mockMvc.perform(get("/api/v1/market/prices/EURUSD"))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("application/json"))
				.andExpect(jsonPath("$.symbol").value("EURUSD"))
				.andExpect(jsonPath("$.bid").value(1.08420))
				.andExpect(jsonPath("$.ask").value(1.08440))
				.andExpect(jsonPath("$.midpoint").value(1.08430))
				.andExpect(jsonPath("$.spread").value(0.00020))
				.andExpect(jsonPath("$.timestamp").value("2026-01-02T03:04:05Z"));
	}

	@Test
	void returnsNotFoundForAnUnsupportedSymbol() throws Exception {
		when(marketDataProvider.getPrice("BTCUSD")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/market/prices/BTCUSD"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY_PAIR"))
				.andExpect(jsonPath("$.message").value("Currency pair is not supported: BTCUSD"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

}

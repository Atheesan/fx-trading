package com.fxai.market.api;

import com.fxai.instrument.Currency;
import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;
import com.fxai.market.error.MarketDataRateLimitException;
import com.fxai.market.error.MarketDataUnavailableException;
import com.fxai.market.error.MarketDataAuthenticationException;
import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.domain.indicator.IndicatorPoint;
import com.fxai.market.domain.indicator.IndicatorResult;
import com.fxai.market.domain.indicator.IndicatorSpec;
import com.fxai.market.domain.indicator.IndicatorStatus;
import com.fxai.market.domain.indicator.MarketIndicators;
import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.service.HistoricalCandleService;
import com.fxai.market.service.LatestPriceService;
import com.fxai.market.service.MarketIndicatorService;
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

@org.springframework.context.annotation.Import(com.fxai.auth.SecurityConfiguration.class)
@WebMvcTest(MarketController.class)
class MarketControllerTests {
    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder;

	private static final Instant TIMESTAMP = Instant.parse("2026-01-02T03:04:05Z");

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MarketDataProvider marketDataProvider;

	@MockitoBean
	private HistoricalCandleService historicalCandleService;

	@MockitoBean
	private LatestPriceService latestPriceService;

	@MockitoBean
	private MarketIndicatorService marketIndicatorService;

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
		when(latestPriceService.getLatestPrice("EURUSD")).thenReturn(Optional.of(quote));

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
		when(latestPriceService.getLatestPrice("BTCUSD")).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/market/prices/BTCUSD"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY_PAIR"))
				.andExpect(jsonPath("$.message").value("Currency pair is not supported: BTCUSD"))
				.andExpect(jsonPath("$.timestamp").exists());
	}

	@Test
	void translatesMarketDataRateLimitsToTooManyRequests() throws Exception {
		when(latestPriceService.getLatestPrice("EURUSD")).thenThrow(new MarketDataRateLimitException());

		mockMvc.perform(get("/api/v1/market/prices/EURUSD"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("MARKET_DATA_RATE_LIMITED"))
				.andExpect(jsonPath("$.message").value("Market data request limit reached; try again later"));
	}

	@Test
	void translatesProviderFailuresToServiceUnavailable() throws Exception {
		when(latestPriceService.getLatestPrice("EURUSD")).thenThrow(new MarketDataUnavailableException());

		mockMvc.perform(get("/api/v1/market/prices/EURUSD"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MARKET_DATA_UNAVAILABLE"))
				.andExpect(jsonPath("$.message").value("Market data is temporarily unavailable"));
	}

	@Test
	void returnsHistoricalCandles() throws Exception {
		CandleQuery query = new CandleQuery(
				CandleTimeframe.ONE_HOUR,
				Instant.parse("2026-01-01T00:00:00Z"),
				Instant.parse("2026-01-02T00:00:00Z"),
				500
		);
		Candle candle = new Candle("EURUSD", CandleTimeframe.ONE_HOUR, TIMESTAMP,
				new BigDecimal("1.1000"), new BigDecimal("1.1020"),
				new BigDecimal("1.0990"), new BigDecimal("1.1010"),
				42L);
		when(historicalCandleService.getHistoricalCandles("EURUSD", query))
				.thenReturn(Optional.of(List.of(candle)));

		mockMvc.perform(get("/api/v1/market/candles/EURUSD")
					.param("timeframe", "1h")
					.param("from", "2026-01-01T00:00:00Z")
					.param("to", "2026-01-02T00:00:00Z")
					.param("limit", "500"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].symbol").value("EURUSD"))
				.andExpect(jsonPath("$[0].timeframe").value("1h"))
				.andExpect(jsonPath("$[0].timestamp").value("2026-01-02T03:04:05Z"))
				.andExpect(jsonPath("$[0].close").value(1.1010))
				.andExpect(jsonPath("$[0].volume").value(42));
	}

	@Test
	void returnsNotFoundForAnUnsupportedCandleSymbol() throws Exception {
		CandleQuery query = new CandleQuery(CandleTimeframe.ONE_HOUR, null, null, 500);
		when(historicalCandleService.getHistoricalCandles("BTCUSD", query)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/market/candles/BTCUSD").param("timeframe", "1h"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY_PAIR"));
	}

	@Test
	void rejectsUnsupportedCandleTimeframe() throws Exception {
		mockMvc.perform(get("/api/v1/market/candles/EURUSD").param("timeframe", "30m"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MARKET_REQUEST"));
	}

	@Test
	void rejectsInvalidCandleDates() throws Exception {
		mockMvc.perform(get("/api/v1/market/candles/EURUSD")
					.param("timeframe", "1h")
					.param("from", "not-a-date"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("from must be an ISO-8601 UTC timestamp"));

		mockMvc.perform(get("/api/v1/market/candles/EURUSD")
					.param("timeframe", "1h")
					.param("from", "2026-01-02T00:00:00Z")
					.param("to", "2026-01-01T00:00:00Z"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("from must be before to"));
	}

	@Test
	void rejectsExcessiveCandleLimit() throws Exception {
		mockMvc.perform(get("/api/v1/market/candles/EURUSD")
					.param("timeframe", "1h")
					.param("limit", "5001"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("limit must be between 1 and 5000"));
	}

	@Test
	void translatesCandleProviderFailuresToServiceUnavailable() throws Exception {
		CandleQuery query = new CandleQuery(CandleTimeframe.ONE_HOUR, null, null, 500);
		when(historicalCandleService.getHistoricalCandles("EURUSD", query))
				.thenThrow(new MarketDataUnavailableException());

		mockMvc.perform(get("/api/v1/market/candles/EURUSD").param("timeframe", "1h"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MARKET_DATA_UNAVAILABLE"));
	}

	@Test
	void translatesAuthenticationFailuresToServiceUnavailable() throws Exception {
		when(latestPriceService.getLatestPrice("EURUSD"))
				.thenThrow(new MarketDataAuthenticationException("OANDA credentials were rejected"));

		mockMvc.perform(get("/api/v1/market/prices/EURUSD"))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.code").value("MARKET_DATA_AUTHENTICATION_FAILED"));
	}

	@Test
	void returnsRequestedTechnicalIndicatorsAndInsufficientDataStatus() throws Exception {
		var smaPoint = IndicatorPoint.value(TIMESTAMP, new BigDecimal("1.10125"));
		var indicators = new MarketIndicators(
				"EURUSD",
				CandleTimeframe.ONE_HOUR,
				List.of(
						new IndicatorResult(
								IndicatorSpec.SMA_20,
								IndicatorStatus.AVAILABLE,
								40,
								List.of(smaPoint)
						),
						IndicatorResult.insufficient(IndicatorSpec.SMA_50, 40)
				)
		);
		when(marketIndicatorService.calculate(
				"EURUSD",
				CandleTimeframe.ONE_HOUR,
				List.of(IndicatorSpec.SMA_20, IndicatorSpec.SMA_50)
		)).thenReturn(Optional.of(indicators));

		mockMvc.perform(get("/api/v1/market/indicators/EURUSD")
					.param("timeframe", "1h")
					.param("indicators", "sma20,sma50"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.symbol").value("EURUSD"))
				.andExpect(jsonPath("$.timeframe").value("1h"))
				.andExpect(jsonPath("$.indicators[0].name").value("sma20"))
				.andExpect(jsonPath("$.indicators[0].status").value("AVAILABLE"))
				.andExpect(jsonPath("$.indicators[0].value").value(1.10125))
				.andExpect(jsonPath("$.indicators[0].points[0].timestamp").value("2026-01-02T03:04:05Z"))
				.andExpect(jsonPath("$.indicators[1].status").value("INSUFFICIENT_DATA"))
				.andExpect(jsonPath("$.indicators[1].requiredDataPoints").value(50))
				.andExpect(jsonPath("$.indicators[1].availableDataPoints").value(40))
				.andExpect(jsonPath("$.indicators[1].value").doesNotExist())
				.andExpect(jsonPath("$.indicators[1].points").isEmpty());
	}

	@Test
	void rejectsUnsupportedIndicators() throws Exception {
		mockMvc.perform(get("/api/v1/market/indicators/EURUSD")
					.param("timeframe", "1h")
					.param("indicators", "bollinger"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_MARKET_REQUEST"))
				.andExpect(jsonPath("$.message").value("Unsupported indicator: bollinger"));
	}

	@Test
	void returnsNotFoundForUnsupportedIndicatorSymbol() throws Exception {
		when(marketIndicatorService.calculate(
				"BTCUSD",
				CandleTimeframe.ONE_HOUR,
				List.of(IndicatorSpec.MACD)
		)).thenReturn(Optional.empty());

		mockMvc.perform(get("/api/v1/market/indicators/BTCUSD")
					.param("timeframe", "1h")
					.param("indicators", "macd"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY_PAIR"));
	}

}

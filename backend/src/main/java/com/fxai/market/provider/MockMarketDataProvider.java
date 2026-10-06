package com.fxai.market.provider;

import com.fxai.instrument.Currency;
import com.fxai.instrument.CurrencyPair;
import com.fxai.market.PriceQuote;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class MockMarketDataProvider implements MarketDataProvider {

	private static final List<CurrencyPair> SUPPORTED_PAIRS = List.of(
			new CurrencyPair(Currency.EUR, Currency.USD),
			new CurrencyPair(Currency.GBP, Currency.USD),
			new CurrencyPair(Currency.USD, Currency.JPY),
			new CurrencyPair(Currency.AUD, Currency.USD),
			new CurrencyPair(Currency.NZD, Currency.USD),
			new CurrencyPair(Currency.USD, Currency.CAD),
			new CurrencyPair(Currency.USD, Currency.CHF),
			new CurrencyPair(Currency.EUR, Currency.GBP)
	);

	private static final Map<String, MockPrice> MOCK_PRICES = Map.ofEntries(
			Map.entry("EURUSD", new MockPrice("1.08420", "1.08440")),
			Map.entry("GBPUSD", new MockPrice("1.26910", "1.26935")),
			Map.entry("USDJPY", new MockPrice("149.620", "149.650")),
			Map.entry("AUDUSD", new MockPrice("0.65840", "0.65860")),
			Map.entry("NZDUSD", new MockPrice("0.61230", "0.61255")),
			Map.entry("USDCAD", new MockPrice("1.35420", "1.35445")),
			Map.entry("USDCHF", new MockPrice("0.88210", "0.88235")),
			Map.entry("EURGBP", new MockPrice("0.85410", "0.85430"))
	);

	private final Clock clock;
	private final Map<String, CurrencyPair> pairsBySymbol;

	public MockMarketDataProvider() {
		this(Clock.systemUTC());
	}

	MockMarketDataProvider(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
		this.pairsBySymbol = SUPPORTED_PAIRS.stream()
				.collect(Collectors.toUnmodifiableMap(CurrencyPair::symbol, Function.identity()));
	}

	@Override
	public List<CurrencyPair> getCurrencyPairs() {
		return SUPPORTED_PAIRS;
	}

	@Override
	public Optional<PriceQuote> getPrice(String symbol) {
		String normalizedSymbol = normalizeSymbol(symbol);
		if (!pairsBySymbol.containsKey(normalizedSymbol)) {
			return Optional.empty();
		}

		MockPrice price = MOCK_PRICES.get(normalizedSymbol);
		return Optional.of(PriceQuote.fromBidAndAsk(
				normalizedSymbol,
				price.bid(),
				price.ask(),
				clock.instant()
		));
	}

	private String normalizeSymbol(String symbol) {
		if (symbol == null) {
			return "";
		}
		return symbol.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "");
	}

	private record MockPrice(BigDecimal bid, BigDecimal ask) {

		private MockPrice(String bid, String ask) {
			this(new BigDecimal(bid), new BigDecimal(ask));
		}

	}

}

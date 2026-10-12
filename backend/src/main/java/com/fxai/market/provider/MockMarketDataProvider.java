package com.fxai.market.provider;

import com.fxai.instrument.CurrencyPair;
import com.fxai.instrument.SupportedCurrencyPairs;
import com.fxai.market.PriceQuote;
import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

@Component
@ConditionalOnProperty(
		prefix = "fxai.market",
		name = "provider",
		havingValue = "mock",
		matchIfMissing = true
)
public class MockMarketDataProvider implements MarketDataProvider {
	@Override
	public Optional<List<Candle>> getHistoricalCandles(String symbol, CandleQuery query) {
		Optional<CurrencyPair> pair = SupportedCurrencyPairs.findBySymbol(symbol);
		if (pair.isEmpty()) {
			return Optional.empty();
		}

		var end = query.to() == null ? clock.instant() : query.to();
		BigDecimal price = new BigDecimal("1.00000");
		List<Candle> candles = IntStream.range(0, query.limit())
				.mapToObj(index -> new Candle(
						pair.orElseThrow().symbol(),
						query.timeframe(),
						end.minus(query.timeframe().duration().multipliedBy(query.limit() - index)),
						price,
						price,
						price,
						price,
						0L
				))
				.filter(candle -> query.from() == null || !candle.timestamp().isBefore(query.from()))
				.toList();
		return Optional.of(candles);
	}

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

	public MockMarketDataProvider() {
		this(Clock.systemUTC());
	}

	MockMarketDataProvider(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public List<CurrencyPair> getCurrencyPairs() {
		return SupportedCurrencyPairs.all();
	}

	@Override
	public Optional<PriceQuote> getPrice(String symbol) {
		Optional<CurrencyPair> currencyPair = SupportedCurrencyPairs.findBySymbol(symbol);
		if (currencyPair.isEmpty()) {
			return Optional.empty();
		}

		String normalizedSymbol = currencyPair.orElseThrow().symbol();
		MockPrice price = MOCK_PRICES.get(normalizedSymbol);
		return Optional.of(PriceQuote.fromBidAndAsk(
				normalizedSymbol,
				price.bid(),
				price.ask(),
				clock.instant()
		));
	}

	private record MockPrice(BigDecimal bid, BigDecimal ask) {

		private MockPrice(String bid, String ask) {
			this(new BigDecimal(bid), new BigDecimal(ask));
		}

	}

}

package com.fxai.market.domain.indicator;

import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleTimeframe;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class TechnicalIndicatorServiceTests {
	private static final Instant START = Instant.parse("2026-01-01T00:00:00Z");
	private final TechnicalIndicatorService service = new TechnicalIndicatorService();

	@Test
	void calculatesSimpleMovingAverageFromKnownValues() {
		IndicatorResult result = service.calculate(
				IndicatorSpec.SMA_20,
				candles(IntStream.rangeClosed(1, 22).mapToObj(BigDecimal::valueOf).toList())
		);

		assertThat(result.status()).isEqualTo(IndicatorStatus.AVAILABLE);
		assertThat(result.points()).hasSize(3);
		assertThat(result.points().get(0).value()).isEqualByComparingTo("10.5");
		assertThat(result.points().get(1).value()).isEqualByComparingTo("11.5");
		assertThat(result.points().get(2).value()).isEqualByComparingTo("12.5");
	}

	@Test
	void calculatesExponentialMovingAverageFromKnownValues() {
		IndicatorResult result = service.calculate(
				IndicatorSpec.EMA_20,
				candles(IntStream.rangeClosed(1, 22).mapToObj(BigDecimal::valueOf).toList())
		);

		assertThat(result.points()).hasSize(3);
		assertThat(result.points().get(0).value()).isEqualByComparingTo("10.5");
		assertThat(result.points().get(1).value()).isEqualByComparingTo("11.5");
		assertThat(result.points().get(2).value()).isEqualByComparingTo("12.5");
	}

	@Test
	void calculatesWilderRelativeStrengthIndexFromKnownValues() {
		List<BigDecimal> closes = List.of(
				"44.34", "44.09", "44.15", "43.61", "44.33",
				"44.83", "45.10", "45.42", "45.84", "46.08",
				"45.89", "46.03", "45.61", "46.28", "46.28"
		).stream().map(BigDecimal::new).toList();

		IndicatorResult result = service.calculate(IndicatorSpec.RSI_14, candles(closes));

		assertThat(result.points()).hasSize(1);
		assertThat(result.points().getFirst().value())
				.isCloseTo(new BigDecimal("70.464135"), within(new BigDecimal("0.000001")));
	}

	@Test
	void calculatesStandardMacdFromKnownLinearValues() {
		IndicatorResult result = service.calculate(
				IndicatorSpec.MACD,
				candles(IntStream.rangeClosed(1, 35).mapToObj(BigDecimal::valueOf).toList())
		);

		assertThat(result.points()).hasSize(2);
		assertThat(result.points()).allSatisfy(point -> {
			assertThat(point.value()).isCloseTo(new BigDecimal("7"), within(new BigDecimal("0.0000000001")));
			assertThat(point.signal()).isCloseTo(new BigDecimal("7"), within(new BigDecimal("0.0000000001")));
			assertThat(point.histogram()).isCloseTo(BigDecimal.ZERO, within(new BigDecimal("0.0000000001")));
		});
	}

	@Test
	void reportsInsufficientHistoricalDataExplicitly() {
		IndicatorResult result = service.calculate(
				IndicatorSpec.SMA_50,
				candles(IntStream.rangeClosed(1, 20).mapToObj(BigDecimal::valueOf).toList())
		);

		assertThat(result.status()).isEqualTo(IndicatorStatus.INSUFFICIENT_DATA);
		assertThat(result.availableDataPoints()).isEqualTo(20);
		assertThat(result.specification().requiredDataPoints()).isEqualTo(50);
		assertThat(result.points()).isEmpty();
	}

	private List<Candle> candles(List<BigDecimal> closes) {
		return IntStream.range(0, closes.size())
				.mapToObj(index -> {
					BigDecimal close = closes.get(index);
					return new Candle(
							"EURUSD",
							CandleTimeframe.ONE_HOUR,
							START.plus(index, ChronoUnit.HOURS),
							close,
							close,
							close,
							close,
							0L
					);
				})
				.toList();
	}
}

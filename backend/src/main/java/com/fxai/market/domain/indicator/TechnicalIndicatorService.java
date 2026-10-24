package com.fxai.market.domain.indicator;

import com.fxai.market.domain.Candle;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public final class TechnicalIndicatorService {
	private static final MathContext MATH_CONTEXT = MathContext.DECIMAL128;
	private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
	private static final BigDecimal FIFTY = BigDecimal.valueOf(50);
	private static final int MACD_FAST_PERIOD = 12;
	private static final int MACD_SLOW_PERIOD = 26;
	private static final int MACD_SIGNAL_PERIOD = 9;

	public IndicatorResult calculate(IndicatorSpec specification, List<Candle> candles) {
		Objects.requireNonNull(specification, "specification must not be null");
		Objects.requireNonNull(candles, "candles must not be null");

		List<Candle> orderedCandles = candles.stream()
				.sorted(Comparator.comparing(Candle::timestamp))
				.toList();
		if (orderedCandles.size() < specification.requiredDataPoints()) {
			return IndicatorResult.insufficient(specification, orderedCandles.size());
		}

		List<IndicatorPoint> points = switch (specification.type()) {
			case SMA -> simpleMovingAverage(orderedCandles, specification.period());
			case EMA -> exponentialMovingAverage(orderedCandles, specification.period());
			case RSI -> relativeStrengthIndex(orderedCandles, specification.period());
			case MACD -> movingAverageConvergenceDivergence(orderedCandles);
		};
		return new IndicatorResult(
				specification,
				IndicatorStatus.AVAILABLE,
				orderedCandles.size(),
				points
		);
	}

	private List<IndicatorPoint> simpleMovingAverage(List<Candle> candles, int period) {
		var points = new java.util.ArrayList<IndicatorPoint>();
		BigDecimal sum = BigDecimal.ZERO;
		for (int index = 0; index < candles.size(); index++) {
			sum = sum.add(candles.get(index).close(), MATH_CONTEXT);
			if (index >= period) {
				sum = sum.subtract(candles.get(index - period).close(), MATH_CONTEXT);
			}
			if (index >= period - 1) {
				points.add(IndicatorPoint.value(
						candles.get(index).timestamp(),
						sum.divide(BigDecimal.valueOf(period), MATH_CONTEXT)
				));
			}
		}
		return List.copyOf(points);
	}

	private List<IndicatorPoint> exponentialMovingAverage(List<Candle> candles, int period) {
		BigDecimal[] values = exponentialMovingAverageValues(candles, period);
		var points = new java.util.ArrayList<IndicatorPoint>();
		for (int index = period - 1; index < candles.size(); index++) {
			points.add(IndicatorPoint.value(candles.get(index).timestamp(), values[index]));
		}
		return List.copyOf(points);
	}

	private BigDecimal[] exponentialMovingAverageValues(List<Candle> candles, int period) {
		BigDecimal[] values = new BigDecimal[candles.size()];
		BigDecimal seed = candles.stream()
				.limit(period)
				.map(Candle::close)
				.reduce(BigDecimal.ZERO, (left, right) -> left.add(right, MATH_CONTEXT))
				.divide(BigDecimal.valueOf(period), MATH_CONTEXT);
		values[period - 1] = seed;
		BigDecimal multiplier = BigDecimal.TWO.divide(BigDecimal.valueOf(period + 1L), MATH_CONTEXT);
		for (int index = period; index < candles.size(); index++) {
			BigDecimal previous = values[index - 1];
			values[index] = candles.get(index).close()
					.subtract(previous, MATH_CONTEXT)
					.multiply(multiplier, MATH_CONTEXT)
					.add(previous, MATH_CONTEXT);
		}
		return values;
	}

	private List<IndicatorPoint> relativeStrengthIndex(List<Candle> candles, int period) {
		BigDecimal gains = BigDecimal.ZERO;
		BigDecimal losses = BigDecimal.ZERO;
		for (int index = 1; index <= period; index++) {
			BigDecimal change = candles.get(index).close().subtract(candles.get(index - 1).close(), MATH_CONTEXT);
			if (change.signum() > 0) {
				gains = gains.add(change, MATH_CONTEXT);
			} else {
				losses = losses.add(change.abs(), MATH_CONTEXT);
			}
		}

		BigDecimal divisor = BigDecimal.valueOf(period);
		BigDecimal averageGain = gains.divide(divisor, MATH_CONTEXT);
		BigDecimal averageLoss = losses.divide(divisor, MATH_CONTEXT);
		var points = new java.util.ArrayList<IndicatorPoint>();
		points.add(IndicatorPoint.value(
				candles.get(period).timestamp(),
				rsiValue(averageGain, averageLoss)
		));

		for (int index = period + 1; index < candles.size(); index++) {
			BigDecimal change = candles.get(index).close().subtract(candles.get(index - 1).close(), MATH_CONTEXT);
			BigDecimal gain = change.signum() > 0 ? change : BigDecimal.ZERO;
			BigDecimal loss = change.signum() < 0 ? change.abs() : BigDecimal.ZERO;
			averageGain = averageGain.multiply(BigDecimal.valueOf(period - 1L), MATH_CONTEXT)
					.add(gain, MATH_CONTEXT)
					.divide(divisor, MATH_CONTEXT);
			averageLoss = averageLoss.multiply(BigDecimal.valueOf(period - 1L), MATH_CONTEXT)
					.add(loss, MATH_CONTEXT)
					.divide(divisor, MATH_CONTEXT);
			points.add(IndicatorPoint.value(
					candles.get(index).timestamp(),
					rsiValue(averageGain, averageLoss)
			));
		}
		return List.copyOf(points);
	}

	private BigDecimal rsiValue(BigDecimal averageGain, BigDecimal averageLoss) {
		if (averageLoss.signum() == 0) {
			return averageGain.signum() == 0 ? FIFTY : ONE_HUNDRED;
		}
		if (averageGain.signum() == 0) {
			return BigDecimal.ZERO;
		}
		BigDecimal relativeStrength = averageGain.divide(averageLoss, MATH_CONTEXT);
		return ONE_HUNDRED.subtract(
				ONE_HUNDRED.divide(BigDecimal.ONE.add(relativeStrength, MATH_CONTEXT), MATH_CONTEXT),
				MATH_CONTEXT
		);
	}

	private List<IndicatorPoint> movingAverageConvergenceDivergence(List<Candle> candles) {
		BigDecimal[] fast = exponentialMovingAverageValues(candles, MACD_FAST_PERIOD);
		BigDecimal[] slow = exponentialMovingAverageValues(candles, MACD_SLOW_PERIOD);
		BigDecimal[] macd = new BigDecimal[candles.size()];
		for (int index = MACD_SLOW_PERIOD - 1; index < candles.size(); index++) {
			macd[index] = fast[index].subtract(slow[index], MATH_CONTEXT);
		}

		int firstSignalIndex = MACD_SLOW_PERIOD + MACD_SIGNAL_PERIOD - 2;
		BigDecimal signal = BigDecimal.ZERO;
		for (int index = MACD_SLOW_PERIOD - 1; index <= firstSignalIndex; index++) {
			signal = signal.add(macd[index], MATH_CONTEXT);
		}
		signal = signal.divide(BigDecimal.valueOf(MACD_SIGNAL_PERIOD), MATH_CONTEXT);
		BigDecimal multiplier = BigDecimal.TWO.divide(
				BigDecimal.valueOf(MACD_SIGNAL_PERIOD + 1L),
				MATH_CONTEXT
		);

		var points = new java.util.ArrayList<IndicatorPoint>();
		points.add(IndicatorPoint.macd(
				candles.get(firstSignalIndex).timestamp(),
				macd[firstSignalIndex],
				signal,
				macd[firstSignalIndex].subtract(signal, MATH_CONTEXT)
		));
		for (int index = firstSignalIndex + 1; index < candles.size(); index++) {
			signal = macd[index]
					.subtract(signal, MATH_CONTEXT)
					.multiply(multiplier, MATH_CONTEXT)
					.add(signal, MATH_CONTEXT);
			points.add(IndicatorPoint.macd(
					candles.get(index).timestamp(),
					macd[index],
					signal,
					macd[index].subtract(signal, MATH_CONTEXT)
			));
		}
		return List.copyOf(points);
	}
}

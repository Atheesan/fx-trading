package com.fxai.market.domain.indicator;

import java.util.List;
import java.util.Objects;

public record IndicatorResult(
		IndicatorSpec specification,
		IndicatorStatus status,
		int availableDataPoints,
		List<IndicatorPoint> points
) {
	public IndicatorResult {
		Objects.requireNonNull(specification, "specification must not be null");
		Objects.requireNonNull(status, "status must not be null");
		points = List.copyOf(points);
	}

	public static IndicatorResult insufficient(IndicatorSpec specification, int availableDataPoints) {
		return new IndicatorResult(
				specification,
				IndicatorStatus.INSUFFICIENT_DATA,
				availableDataPoints,
				List.of()
		);
	}
}

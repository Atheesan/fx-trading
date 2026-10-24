package com.fxai.market.api;

import com.fxai.market.domain.indicator.IndicatorPoint;
import com.fxai.market.domain.indicator.IndicatorResult;

import java.math.BigDecimal;
import java.util.List;

public record IndicatorResponse(
		String name,
		String type,
		String status,
		Integer period,
		int requiredDataPoints,
		int availableDataPoints,
		BigDecimal value,
		BigDecimal signal,
		BigDecimal histogram,
		List<IndicatorPointResponse> points
) {
	static IndicatorResponse from(IndicatorResult result) {
		IndicatorPoint latest = result.points().isEmpty() ? null : result.points().getLast();
		return new IndicatorResponse(
				result.specification().requestName(),
				result.specification().type().name(),
				result.status().name(),
				result.specification().period(),
				result.specification().requiredDataPoints(),
				result.availableDataPoints(),
				latest == null ? null : latest.value(),
				latest == null ? null : latest.signal(),
				latest == null ? null : latest.histogram(),
				result.points().stream().map(IndicatorPointResponse::from).toList()
		);
	}
}

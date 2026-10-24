package com.fxai.alert.api;

import com.fxai.alert.domain.PriceAlert;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PriceAlertResponse(
		UUID id,
		String symbol,
		String condition,
		BigDecimal targetPrice,
		String status,
		Instant createdAt,
		Instant triggeredAt
) {
	static PriceAlertResponse from(PriceAlert alert) {
		return new PriceAlertResponse(
				alert.id(),
				alert.symbol(),
				alert.condition().name(),
				alert.targetPrice(),
				alert.status().name(),
				alert.createdAt(),
				alert.triggeredAt()
		);
	}
}

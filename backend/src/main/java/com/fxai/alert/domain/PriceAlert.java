package com.fxai.alert.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record PriceAlert(
		UUID id,
		String symbol,
		AlertCondition condition,
		BigDecimal targetPrice,
		AlertStatus status,
		Instant createdAt,
		Instant triggeredAt
) {
	public PriceAlert {
		Objects.requireNonNull(id, "id must not be null");
		if (symbol == null || symbol.isBlank()) {
			throw new IllegalArgumentException("symbol must not be blank");
		}
		Objects.requireNonNull(condition, "condition must not be null");
		Objects.requireNonNull(targetPrice, "targetPrice must not be null");
		Objects.requireNonNull(status, "status must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
		if (targetPrice.signum() <= 0) {
			throw new IllegalArgumentException("targetPrice must be greater than zero");
		}
		if (status == AlertStatus.ACTIVE && triggeredAt != null) {
			throw new IllegalArgumentException("an active alert cannot have a trigger timestamp");
		}
		if (status == AlertStatus.TRIGGERED && triggeredAt == null) {
			throw new IllegalArgumentException("a triggered alert must have a trigger timestamp");
		}
	}

	public static PriceAlert active(
			UUID id,
			String symbol,
			AlertCondition condition,
			BigDecimal targetPrice,
			Instant createdAt
	) {
		return new PriceAlert(
				id,
				symbol,
				condition,
				targetPrice,
				AlertStatus.ACTIVE,
				createdAt,
				null
		);
	}

	public PriceAlert trigger(Instant timestamp) {
		if (status == AlertStatus.TRIGGERED) {
			return this;
		}
		return new PriceAlert(
				id,
				symbol,
				condition,
				targetPrice,
				AlertStatus.TRIGGERED,
				createdAt,
				Objects.requireNonNull(timestamp, "timestamp must not be null")
		);
	}
}

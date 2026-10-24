package com.fxai.alert.domain;

import java.math.BigDecimal;
import java.util.Objects;

public final class PriceAlertEvaluator {
	public boolean isSatisfied(PriceAlert alert, BigDecimal currentPrice) {
		Objects.requireNonNull(alert, "alert must not be null");
		Objects.requireNonNull(currentPrice, "currentPrice must not be null");
		if (alert.status() != AlertStatus.ACTIVE) {
			return false;
		}

		int comparison = currentPrice.compareTo(alert.targetPrice());
		return switch (alert.condition()) {
			case PRICE_ABOVE -> comparison >= 0;
			case PRICE_BELOW -> comparison <= 0;
		};
	}
}

package com.fxai.alert.api;

import com.fxai.alert.domain.AlertCondition;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePriceAlertRequest(
		@NotBlank String symbol,
		@NotNull AlertCondition condition,
		@NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal targetPrice
) {
}

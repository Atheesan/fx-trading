package com.fxai.alert.persistence;

import com.fxai.alert.domain.AlertCondition;
import com.fxai.alert.domain.AlertStatus;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.instrument.persistence.CurrencyPairEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "price_alert")
public class PriceAlertEntity {
	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "currency_pair_symbol", nullable = false)
	private CurrencyPairEntity currencyPair;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private AlertCondition condition;

	@Column(name = "target_price", precision = 19, scale = 8, nullable = false)
	private BigDecimal targetPrice;

	@Enumerated(EnumType.STRING)
	@Column(length = 20, nullable = false)
	private AlertStatus status;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "triggered_at")
	private Instant triggeredAt;

	protected PriceAlertEntity() {
	}

	PriceAlertEntity(PriceAlert alert, CurrencyPairEntity currencyPair) {
		this.id = alert.id();
		this.currencyPair = currencyPair;
		this.condition = alert.condition();
		this.targetPrice = alert.targetPrice();
		this.status = alert.status();
		this.createdAt = alert.createdAt();
		this.triggeredAt = alert.triggeredAt();
	}

	public PriceAlert toDomain() {
		return new PriceAlert(
				id,
				currencyPair.getSymbol(),
				condition,
				targetPrice,
				status,
				createdAt,
				triggeredAt
		);
	}
}

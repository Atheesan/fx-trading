package com.fxai.alert.service;

import com.fxai.alert.domain.AlertCondition;
import com.fxai.alert.domain.AlertStatus;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.alert.repository.InMemoryPriceAlertRepository;
import com.fxai.alert.repository.PriceAlertRepository;
import com.fxai.market.PriceQuote;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PriceAlertEvaluationServiceTests {
	private static final Instant CREATED_AT = Instant.parse("2026-01-02T00:00:00Z");
	private static final Instant QUOTE_TIME = Instant.parse("2026-01-02T03:04:05Z");

	private PriceAlertRepository repository;
	private PriceAlertEvaluationService service;

	@BeforeEach
	void setUp() {
		repository = spy(new InMemoryPriceAlertRepository());
		service = new PriceAlertEvaluationService(repository);
	}

	@Test
	void triggersWhenPriceIsGreaterThanTarget() {
		PriceAlert alert = saveAlert(AlertCondition.PRICE_ABOVE, "1.1000");

		service.evaluate(quote("1.1100"));

		assertTriggered(alert.id());
	}

	@Test
	void triggersWhenPriceIsLowerThanTarget() {
		PriceAlert alert = saveAlert(AlertCondition.PRICE_BELOW, "1.1000");

		service.evaluate(quote("1.0900"));

		assertTriggered(alert.id());
	}

	@Test
	void exactBoundaryTriggersForBothConditions() {
		PriceAlert above = saveAlert(AlertCondition.PRICE_ABOVE, "1.1000");
		PriceAlert below = saveAlert(AlertCondition.PRICE_BELOW, "1.1000");

		service.evaluate(quote("1.1000"));

		assertTriggered(above.id());
		assertTriggered(below.id());
	}

	@Test
	void previouslyTriggeredAlertIsNotTriggeredAgain() {
		PriceAlert alert = saveAlert(AlertCondition.PRICE_ABOVE, "1.1000");
		Instant firstTrigger = Instant.parse("2026-01-02T01:00:00Z");
		repository.markTriggered(alert.id(), firstTrigger);

		service.evaluate(quote("1.1200"));

		PriceAlert stored = find(alert.id());
		assertThat(stored.triggeredAt()).isEqualTo(firstTrigger);
		verify(repository, never()).markTriggered(alert.id(), QUOTE_TIME);
	}

	@Test
	void duplicateQuoteProcessingTriggersAlertOnlyOnce() {
		PriceAlert alert = saveAlert(AlertCondition.PRICE_ABOVE, "1.1000");
		PriceQuote quote = quote("1.1200");

		service.evaluate(quote);
		service.evaluate(quote);

		assertTriggered(alert.id());
		verify(repository, times(1)).markTriggered(alert.id(), QUOTE_TIME);
	}

	private PriceAlert saveAlert(AlertCondition condition, String targetPrice) {
		return repository.save(PriceAlert.active(
				UUID.randomUUID(),
				"EURUSD",
				condition,
				new BigDecimal(targetPrice),
				CREATED_AT
		));
	}

	private PriceQuote quote(String midpoint) {
		BigDecimal price = new BigDecimal(midpoint);
		return PriceQuote.fromBidAndAsk("EURUSD", price, price, QUOTE_TIME);
	}

	private void assertTriggered(UUID id) {
		PriceAlert stored = find(id);
		assertThat(stored.status()).isEqualTo(AlertStatus.TRIGGERED);
		assertThat(stored.triggeredAt()).isEqualTo(QUOTE_TIME);
	}

	private PriceAlert find(UUID id) {
		return repository.findAll().stream()
				.filter(alert -> alert.id().equals(id))
				.findFirst()
				.orElseThrow();
	}
}

package com.fxai.persistence;

import com.fxai.PostgresIntegrationTest;
import com.fxai.alert.domain.AlertCondition;
import com.fxai.alert.domain.AlertStatus;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.alert.repository.PriceAlertRepository;
import com.fxai.instrument.persistence.CurrencyPairJpaRepository;
import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.repository.HistoricalCandleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceRepositoryIntegrationTests extends PostgresIntegrationTest {
	@Autowired
	private CurrencyPairJpaRepository currencyPairs;

	@Autowired
	private PriceAlertRepository priceAlerts;

	@Autowired
	private HistoricalCandleRepository historicalCandles;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void flywaySeedsSupportedCurrencyPairsAndCreatesCandleHypertable() {
		assertThat(currencyPairs.findAll())
				.extracting(entity -> entity.toDomain().symbol())
				.containsExactlyInAnyOrder(
						"EURUSD", "GBPUSD", "USDJPY", "AUDUSD",
						"NZDUSD", "USDCAD", "USDCHF", "EURGBP"
				);

		Integer hypertables = jdbcTemplate.queryForObject("""
				select count(*)
				from timescaledb_information.hypertables
				where hypertable_name = 'historical_candle'
				""", Integer.class);
		assertThat(hypertables).isEqualTo(1);
	}

	@Test
	void persistsAndAtomicallyTriggersPriceAlerts() {
		UUID id = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-04-01T01:02:03Z");
		Instant triggeredAt = Instant.parse("2026-04-01T02:03:04Z");
		PriceAlert alert = PriceAlert.active(
				id,
				"EURUSD",
				AlertCondition.PRICE_ABOVE,
				new BigDecimal("1.12500000"),
				createdAt
		);

		priceAlerts.save(alert);

		assertThat(priceAlerts.findActiveBySymbol("EURUSD"))
				.extracting(PriceAlert::id)
				.contains(id);
		PriceAlert triggered = priceAlerts.markTriggered(id, triggeredAt).orElseThrow();
		assertThat(triggered.status()).isEqualTo(AlertStatus.TRIGGERED);
		assertThat(triggered.triggeredAt()).isEqualTo(triggeredAt);
		assertThat(priceAlerts.markTriggered(id, triggeredAt.plusSeconds(1))).isEmpty();
	}

	@Test
	void persistsAndQueriesHistoricalCandlesByPairAndTimeframe() {
		Instant firstTimestamp = Instant.parse("2026-05-01T00:00:00Z");
		Instant secondTimestamp = Instant.parse("2026-05-01T01:00:00Z");
		historicalCandles.saveAll(List.of(
				candle(firstTimestamp, "1.1000000000"),
				candle(secondTimestamp, "1.1010000000")
		));

		List<Candle> stored = historicalCandles.find(
				"EURUSD",
				new CandleQuery(
						CandleTimeframe.ONE_HOUR,
						firstTimestamp,
						secondTimestamp,
						10
				)
		);

		assertThat(stored).extracting(Candle::timestamp)
				.containsExactly(firstTimestamp, secondTimestamp);
		assertThat(stored).extracting(Candle::close)
				.containsExactly(new BigDecimal("1.1000000000"), new BigDecimal("1.1010000000"));
	}

	private Candle candle(Instant timestamp, String priceValue) {
		BigDecimal price = new BigDecimal(priceValue);
		return new Candle(
				"EURUSD",
				CandleTimeframe.ONE_HOUR,
				timestamp,
				price,
				price,
				price,
				price,
				100L
		);
	}
}

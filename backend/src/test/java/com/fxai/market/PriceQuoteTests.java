package com.fxai.market;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class PriceQuoteTests {

	private static final Instant TIMESTAMP = Instant.parse("2026-01-02T03:04:05Z");

	@Test
	void calculatesMidpointAndSpreadFromBidAndAsk() {
		PriceQuote quote = PriceQuote.fromBidAndAsk(
				"EURUSD",
				new BigDecimal("1.08420"),
				new BigDecimal("1.08440"),
				TIMESTAMP
		);

		assertThat(quote.midpoint()).isEqualByComparingTo("1.08430");
		assertThat(quote.spread()).isEqualByComparingTo("0.00020");
		assertThat(quote.timestamp()).isEqualTo(TIMESTAMP);
	}

	@Test
	void rejectsABidGreaterThanTheAsk() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> PriceQuote.fromBidAndAsk(
						"EURUSD",
						new BigDecimal("1.10000"),
						new BigDecimal("1.09000"),
						TIMESTAMP
				))
				.withMessageContaining("bid must not be greater than ask");
	}

}

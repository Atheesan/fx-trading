package com.fxai.instrument;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class CurrencyPairTests {

	@Test
	void derivesTheCanonicalSymbolAndDisplayName() {
		CurrencyPair pair = new CurrencyPair(Currency.EUR, Currency.USD);

		assertThat(pair.symbol()).isEqualTo("EURUSD");
		assertThat(pair.displayName()).isEqualTo("EUR/USD");
	}

	@Test
	void rejectsAPairContainingTheSameCurrencyTwice() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new CurrencyPair(Currency.USD, Currency.USD))
				.withMessageContaining("different currencies");
	}

}

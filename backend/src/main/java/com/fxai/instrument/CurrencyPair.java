package com.fxai.instrument;

import java.util.Objects;

public record CurrencyPair(
		Currency baseCurrency,
		Currency quoteCurrency,
		String symbol,
		String displayName
) {

	public CurrencyPair(Currency baseCurrency, Currency quoteCurrency) {
		this(
				baseCurrency,
				quoteCurrency,
				createSymbol(baseCurrency, quoteCurrency),
				createDisplayName(baseCurrency, quoteCurrency)
		);
	}

	public CurrencyPair {
		Objects.requireNonNull(baseCurrency, "baseCurrency must not be null");
		Objects.requireNonNull(quoteCurrency, "quoteCurrency must not be null");
		Objects.requireNonNull(symbol, "symbol must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");

		if (baseCurrency == quoteCurrency) {
			throw new IllegalArgumentException("A currency pair must contain two different currencies");
		}
		if (!symbol.equals(createSymbol(baseCurrency, quoteCurrency))) {
			throw new IllegalArgumentException("symbol must match the currency pair");
		}
		if (!displayName.equals(createDisplayName(baseCurrency, quoteCurrency))) {
			throw new IllegalArgumentException("displayName must match the currency pair");
		}
	}

	private static String createSymbol(Currency baseCurrency, Currency quoteCurrency) {
		return Objects.requireNonNull(baseCurrency, "baseCurrency must not be null").code()
				+ Objects.requireNonNull(quoteCurrency, "quoteCurrency must not be null").code();
	}

	private static String createDisplayName(Currency baseCurrency, Currency quoteCurrency) {
		return Objects.requireNonNull(baseCurrency, "baseCurrency must not be null").code()
				+ "/" + Objects.requireNonNull(quoteCurrency, "quoteCurrency must not be null").code();
	}

}

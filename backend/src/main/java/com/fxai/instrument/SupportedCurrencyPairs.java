package com.fxai.instrument;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class SupportedCurrencyPairs {

	private static final List<CurrencyPair> PAIRS = List.of(
			new CurrencyPair(Currency.EUR, Currency.USD),
			new CurrencyPair(Currency.GBP, Currency.USD),
			new CurrencyPair(Currency.USD, Currency.JPY),
			new CurrencyPair(Currency.AUD, Currency.USD),
			new CurrencyPair(Currency.NZD, Currency.USD),
			new CurrencyPair(Currency.USD, Currency.CAD),
			new CurrencyPair(Currency.USD, Currency.CHF),
			new CurrencyPair(Currency.EUR, Currency.GBP)
	);

	private SupportedCurrencyPairs() {
	}

	public static List<CurrencyPair> all() {
		return PAIRS;
	}

	public static Optional<CurrencyPair> findBySymbol(String symbol) {
		String normalizedSymbol = normalizeSymbol(symbol);
		return PAIRS.stream()
				.filter(pair -> pair.symbol().equals(normalizedSymbol))
				.findFirst();
	}

	private static String normalizeSymbol(String symbol) {
		if (symbol == null) {
			return "";
		}
		return symbol.trim().toUpperCase(Locale.ROOT).replace("-", "").replace("_", "");
	}

}

package com.fxai.market.api;

final class UnsupportedCurrencyPairException extends RuntimeException {

	UnsupportedCurrencyPairException(String symbol) {
		super("Currency pair is not supported: " + symbol);
	}

}

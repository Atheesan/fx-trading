package com.fxai.alert.api;

public final class UnsupportedAlertSymbolException extends RuntimeException {
	public UnsupportedAlertSymbolException(String symbol) {
		super("Currency pair is not supported: " + symbol);
	}
}

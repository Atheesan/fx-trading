package com.fxai.market.error;

public class MarketDataUnavailableException extends RuntimeException {

	public MarketDataUnavailableException() {
		super("Market data is temporarily unavailable");
	}

	public MarketDataUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

}

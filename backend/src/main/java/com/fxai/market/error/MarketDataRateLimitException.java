package com.fxai.market.error;

public class MarketDataRateLimitException extends RuntimeException {

	public MarketDataRateLimitException() {
		super("Market data request limit reached; try again later");
	}

	public MarketDataRateLimitException(String message) {
		super(message);
	}

}

package com.fxai.market.api;

import com.fxai.instrument.CurrencyPair;

public record CurrencyPairResponse(
		String baseCurrency,
		String quoteCurrency,
		String symbol,
		String displayName
) {

	static CurrencyPairResponse from(CurrencyPair pair) {
		return new CurrencyPairResponse(
				pair.baseCurrency().code(),
				pair.quoteCurrency().code(),
				pair.symbol(),
				pair.displayName()
		);
	}

}

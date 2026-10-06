package com.fxai.market.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = MarketController.class)
class MarketApiExceptionHandler {

	@ExceptionHandler(UnsupportedCurrencyPairException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ApiErrorResponse handleUnsupportedCurrencyPair(UnsupportedCurrencyPairException exception) {
		return new ApiErrorResponse("UNSUPPORTED_CURRENCY_PAIR", exception.getMessage(), Instant.now());
	}

}

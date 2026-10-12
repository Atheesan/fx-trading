package com.fxai.market.api;

import com.fxai.market.error.MarketDataRateLimitException;
import com.fxai.market.error.MarketDataUnavailableException;
import com.fxai.market.error.MarketDataAuthenticationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = MarketController.class)
class MarketApiExceptionHandler {

	@ExceptionHandler(InvalidMarketRequestException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	ApiErrorResponse handleInvalidMarketRequest(InvalidMarketRequestException exception) {
		return new ApiErrorResponse("INVALID_MARKET_REQUEST", exception.getMessage(), Instant.now());
	}

	@ExceptionHandler(UnsupportedCurrencyPairException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	ApiErrorResponse handleUnsupportedCurrencyPair(UnsupportedCurrencyPairException exception) {
		return new ApiErrorResponse("UNSUPPORTED_CURRENCY_PAIR", exception.getMessage(), Instant.now());
	}

	@ExceptionHandler(MarketDataRateLimitException.class)
	@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
	ApiErrorResponse handleMarketDataRateLimit(MarketDataRateLimitException exception) {
		return new ApiErrorResponse("MARKET_DATA_RATE_LIMITED", exception.getMessage(), Instant.now());
	}

	@ExceptionHandler(MarketDataUnavailableException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	ApiErrorResponse handleMarketDataUnavailable(MarketDataUnavailableException exception) {
		return new ApiErrorResponse("MARKET_DATA_UNAVAILABLE", exception.getMessage(), Instant.now());
	}

	@ExceptionHandler(MarketDataAuthenticationException.class)
	@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	ApiErrorResponse handleMarketDataAuthentication(MarketDataAuthenticationException exception) {
		return new ApiErrorResponse("MARKET_DATA_AUTHENTICATION_FAILED", exception.getMessage(), Instant.now());
	}

}

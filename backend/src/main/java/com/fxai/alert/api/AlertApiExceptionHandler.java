package com.fxai.alert.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice(assignableTypes = AlertController.class)
class AlertApiExceptionHandler {
	@ExceptionHandler(UnsupportedAlertSymbolException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	AlertApiErrorResponse handleUnsupportedSymbol(UnsupportedAlertSymbolException exception) {
		return new AlertApiErrorResponse("UNSUPPORTED_CURRENCY_PAIR", exception.getMessage(), Instant.now());
	}

	@ExceptionHandler(AlertNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	AlertApiErrorResponse handleNotFound(AlertNotFoundException exception) {
		return new AlertApiErrorResponse("ALERT_NOT_FOUND", exception.getMessage(), Instant.now());
	}
}

package com.fxai.alert.api;

import java.util.UUID;

public final class AlertNotFoundException extends RuntimeException {
	public AlertNotFoundException(UUID id) {
		super("Price alert was not found: " + id);
	}
}

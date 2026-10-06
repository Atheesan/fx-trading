package com.fxai.instrument;

public enum Currency {

	AUD("Australian Dollar"),
	CAD("Canadian Dollar"),
	CHF("Swiss Franc"),
	EUR("Euro"),
	GBP("British Pound"),
	JPY("Japanese Yen"),
	NZD("New Zealand Dollar"),
	USD("US Dollar");

	private final String displayName;

	Currency(String displayName) {
		this.displayName = displayName;
	}

	public String code() {
		return name();
	}

	public String displayName() {
		return displayName;
	}

}

package com.fxai.auth;

public class AuthFailure extends RuntimeException {
	public AuthFailure() {
		super("Authentication failed");
	}
}

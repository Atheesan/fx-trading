package com.fxai.market.provider.oanda.exception;

public final class OandaInvalidResponseException extends OandaProviderException {
    public OandaInvalidResponseException(String message) { super(message); }
    public OandaInvalidResponseException(String message, Throwable cause) { super(message, cause); }
}

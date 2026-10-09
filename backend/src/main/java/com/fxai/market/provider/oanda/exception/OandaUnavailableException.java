package com.fxai.market.provider.oanda.exception;

public final class OandaUnavailableException extends OandaProviderException {
    public OandaUnavailableException(String message) { super(message); }
    public OandaUnavailableException(String message, Throwable cause) { super(message, cause); }
}

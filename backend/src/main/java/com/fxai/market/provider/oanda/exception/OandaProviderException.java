package com.fxai.market.provider.oanda.exception;

public abstract class OandaProviderException extends RuntimeException {
    protected OandaProviderException(String message) { super(message); }
    protected OandaProviderException(String message, Throwable cause) { super(message, cause); }
}

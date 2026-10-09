package com.fxai.market.provider.oanda.exception;

public final class OandaAuthenticationException extends OandaProviderException {
    public OandaAuthenticationException() { super("OANDA authentication failed"); }
}

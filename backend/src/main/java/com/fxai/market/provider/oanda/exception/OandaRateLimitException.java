package com.fxai.market.provider.oanda.exception;

public final class OandaRateLimitException extends OandaProviderException {
    public OandaRateLimitException() { super("OANDA request rate limit exceeded"); }
}

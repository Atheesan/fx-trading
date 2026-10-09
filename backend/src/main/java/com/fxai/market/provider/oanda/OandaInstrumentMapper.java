package com.fxai.market.provider.oanda;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class OandaInstrumentMapper {
    private static final Map<String, String> INSTRUMENTS = Map.of(
            "EURUSD", "EUR_USD", "GBPUSD", "GBP_USD", "USDJPY", "USD_JPY",
            "AUDUSD", "AUD_USD", "NZDUSD", "NZD_USD", "USDCAD", "USD_CAD",
            "USDCHF", "USD_CHF", "EURGBP", "EUR_GBP");

    private OandaInstrumentMapper() {}

    public static Optional<String> toOanda(String symbol) {
        return symbol == null ? Optional.empty()
                : Optional.ofNullable(INSTRUMENTS.get(symbol.toUpperCase(Locale.ROOT)));
    }

    public static Optional<String> toFxai(String instrument) {
        return INSTRUMENTS.entrySet().stream()
                .filter(entry -> entry.getValue().equals(instrument))
                .map(Map.Entry::getKey)
                .findFirst();
    }
}

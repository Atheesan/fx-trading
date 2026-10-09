package com.fxai.market.provider.oanda;

import com.fxai.market.domain.CandleTimeframe;

public final class OandaGranularityMapper {
    private OandaGranularityMapper() {}

    public static String toOanda(CandleTimeframe timeframe) {
        return switch (timeframe) {
            case ONE_MINUTE -> "M1";
            case FIVE_MINUTES -> "M5";
            case FIFTEEN_MINUTES -> "M15";
            case ONE_HOUR -> "H1";
            case ONE_DAY -> "D";
        };
    }
}

package com.fxai.market.provider.oanda;

import static org.assertj.core.api.Assertions.assertThat;

import com.fxai.market.domain.CandleTimeframe;
import org.junit.jupiter.api.Test;

class OandaMappingsTests {
    @Test
    void mapsFxaiSymbolsToOandaInstruments() {
        assertThat(OandaInstrumentMapper.toOanda("EURUSD")).contains("EUR_USD");
        assertThat(OandaInstrumentMapper.toOanda("gbpusd")).contains("GBP_USD");
        assertThat(OandaInstrumentMapper.toOanda("USDJPY")).contains("USD_JPY");
        assertThat(OandaInstrumentMapper.toOanda("BTCUSD")).isEmpty();
    }

    @Test
    void mapsEverySupportedTimeframe() {
        assertThat(OandaGranularityMapper.toOanda(CandleTimeframe.ONE_MINUTE)).isEqualTo("M1");
        assertThat(OandaGranularityMapper.toOanda(CandleTimeframe.FIVE_MINUTES)).isEqualTo("M5");
        assertThat(OandaGranularityMapper.toOanda(CandleTimeframe.FIFTEEN_MINUTES)).isEqualTo("M15");
        assertThat(OandaGranularityMapper.toOanda(CandleTimeframe.ONE_HOUR)).isEqualTo("H1");
        assertThat(OandaGranularityMapper.toOanda(CandleTimeframe.ONE_DAY)).isEqualTo("D");
    }
}

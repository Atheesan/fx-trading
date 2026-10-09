package com.fxai.market.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fxai.market.domain.CandleTimeframe;
import com.fxai.market.error.MarketDataAuthenticationException;
import com.fxai.market.provider.oanda.OandaMarketDataProperties;
import com.fxai.market.provider.oanda.OandaRestApiClient;
import com.fxai.market.provider.oanda.dto.OandaCandleDto;
import com.fxai.market.provider.oanda.dto.OandaCandlesResponse;
import com.fxai.market.provider.oanda.dto.OandaCandlestickDataDto;
import com.fxai.market.provider.oanda.dto.OandaClientPriceDto;
import com.fxai.market.provider.oanda.dto.OandaPriceBucketDto;
import com.fxai.market.provider.oanda.dto.OandaPricingResponse;
import com.fxai.market.provider.oanda.exception.OandaAuthenticationException;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class OandaMarketDataProviderTests {
    @Mock private OandaRestApiClient client;
    private OandaMarketDataProvider provider;

    @BeforeEach
    void setUp() {
        provider = new OandaMarketDataProvider(client, new OandaMarketDataProperties(
                URI.create("https://api-fxpractice.oanda.com"), "test-token", "test-account",
                Duration.ofSeconds(2), Duration.ofSeconds(5)));
    }

    @Test
    void mapsCurrentPriceIntoFxaiDomain() {
        when(client.fetchCurrentPrice("test-account", "EUR_USD")).thenReturn(new OandaPricingResponse(List.of(
                new OandaClientPriceDto("EUR_USD", "2026-10-06T01:02:03Z", "tradeable", true,
                        List.of(new OandaPriceBucketDto("1.10000")),
                        List.of(new OandaPriceBucketDto("1.10020"))))));

        var quote = provider.getPrice("eurusd").orElseThrow();

        assertThat(quote.symbol()).isEqualTo("EURUSD");
        assertThat(quote.bid()).isEqualByComparingTo("1.10000");
        assertThat(quote.ask()).isEqualByComparingTo("1.10020");
        assertThat(quote.midpoint()).isEqualByComparingTo("1.10010");
        assertThat(quote.spread()).isEqualByComparingTo("0.00020");
    }

    @Test
    void mapsHistoricalCandlesAndTimeframe() {
        when(client.fetchCandles("GBP_USD", "M15", 1)).thenReturn(
                new OandaCandlesResponse("GBP_USD", "M15", List.of(new OandaCandleDto(
                        "2026-10-06T01:00:00Z",
                        new OandaCandlestickDataDto("1.3000", "1.3020", "1.2990", "1.3010"),
                        42, true))));

        var candles = provider.getHistoricalCandles("GBPUSD", CandleTimeframe.FIFTEEN_MINUTES, 1)
                .orElseThrow();

        assertThat(candles).singleElement().satisfies(candle -> {
            assertThat(candle.symbol()).isEqualTo("GBPUSD");
            assertThat(candle.timeframe()).isEqualTo(CandleTimeframe.FIFTEEN_MINUTES);
            assertThat(candle.close()).isEqualByComparingTo("1.3010");
            assertThat(candle.volume()).isEqualTo(42);
        });
    }

    @Test
    void rejectsUnsupportedSymbolWithoutCallingOanda() {
        assertThat(provider.getPrice("BTCUSD")).isEmpty();
        verifyNoInteractions(client);
    }

    @Test
    void translatesAuthenticationFailure() {
        when(client.fetchCurrentPrice("test-account", "EUR_USD"))
                .thenThrow(new OandaAuthenticationException());
        assertThatThrownBy(() -> provider.getPrice("EURUSD"))
                .isInstanceOf(MarketDataAuthenticationException.class);
    }
}

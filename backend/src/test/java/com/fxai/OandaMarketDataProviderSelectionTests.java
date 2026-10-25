package com.fxai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.provider.OandaMarketDataProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "fxai.market.provider=oanda",
        "fxai.market.oanda.api-token=test-token",
        "fxai.market.oanda.account-id=test-account"
})
class OandaMarketDataProviderSelectionTests extends PostgresIntegrationTest {
    @Autowired private MarketDataProvider marketDataProvider;

    @Test
    void selectsOandaProviderFromConfiguration() {
        assertThat(marketDataProvider).isInstanceOf(OandaMarketDataProvider.class);
    }
}

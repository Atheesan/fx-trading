package com.fxai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fxai.market.provider.MarketDataProvider;
import com.fxai.market.provider.MockMarketDataProvider;

import static org.assertj.core.api.Assertions.assertThat;

class FxaiApplicationTests extends PostgresIntegrationTest {

	@Autowired
	private MarketDataProvider marketDataProvider;

	@Test
	void contextLoads() {
		assertThat(marketDataProvider).isInstanceOf(MockMarketDataProvider.class);
	}

}

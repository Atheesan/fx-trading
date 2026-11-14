package com.fxai.market.service;

import com.fxai.market.messaging.PriceTickEvent;
import com.fxai.market.messaging.PriceTickPublisher;
import com.fxai.market.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PriceIngestionService {
	private static final Logger LOGGER = LoggerFactory.getLogger(PriceIngestionService.class);
	private final MarketDataProvider provider;
	private final PriceTickPublisher publisher;
	private final String source;

	public PriceIngestionService(MarketDataProvider provider, PriceTickPublisher publisher,
			@Value("${fxai.market.provider:mock}") String source) {
		this.provider = provider;
		this.publisher = publisher;
		this.source = source;
	}

	public void ingestPrices() {
		for (var pair : provider.getCurrencyPairs()) {
			try {
				provider.getPrice(pair.symbol()).map(quote -> PriceTickEvent.from(quote, source))
						.ifPresent(publisher::publish);
			} catch (RuntimeException exception) {
				LOGGER.atWarn().addKeyValue("symbol", pair.symbol())
						.addKeyValue("error_type", exception.getClass().getSimpleName())
						.log("Market price ingestion failed; next poll will fetch a fresh quote");
			}
		}
	}
}

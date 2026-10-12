package com.fxai.market.realtime;

import com.fxai.instrument.CurrencyPair;
import com.fxai.market.provider.MarketDataProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PricePollingService {

	private static final Logger LOGGER = LoggerFactory.getLogger(PricePollingService.class);

	private final MarketDataProvider marketDataProvider;
	private final ApplicationEventPublisher eventPublisher;

	public PricePollingService(
			MarketDataProvider marketDataProvider,
			ApplicationEventPublisher eventPublisher
	) {
		this.marketDataProvider = marketDataProvider;
		this.eventPublisher = eventPublisher;
	}

	@Scheduled(
			initialDelayString = "${fxai.market.price-poll-interval:15000}",
			fixedDelayString = "${fxai.market.price-poll-interval:15000}"
	)
	void pollPrices() {
		for (CurrencyPair pair : marketDataProvider.getCurrencyPairs()) {
			try {
				marketDataProvider.getPrice(pair.symbol())
						.map(PriceQuoteUpdatedEvent::new)
						.ifPresent(eventPublisher::publishEvent);
			} catch (RuntimeException exception) {
				LOGGER.atWarn()
						.addKeyValue("symbol", pair.symbol())
						.addKeyValue("error_type", exception.getClass().getSimpleName())
						.log("Scheduled market price polling failed");
			}
		}
	}

}

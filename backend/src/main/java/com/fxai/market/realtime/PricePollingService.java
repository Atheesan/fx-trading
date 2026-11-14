package com.fxai.market.realtime;

import com.fxai.market.service.PriceIngestionService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PricePollingService {

	private final PriceIngestionService ingestionService;

	public PricePollingService(PriceIngestionService ingestionService) {
		this.ingestionService = ingestionService;
	}

	@Scheduled(
			initialDelayString = "${fxai.market.price-poll-interval:15000}",
			fixedDelayString = "${fxai.market.price-poll-interval:15000}"
	)
	void pollPrices() {
		ingestionService.ingestPrices();
	}

}

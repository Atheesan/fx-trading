package com.fxai.alert.service;

import com.fxai.market.realtime.PriceQuoteUpdatedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class PriceAlertQuoteListener {
	private final PriceAlertEvaluationService evaluationService;

	public PriceAlertQuoteListener(PriceAlertEvaluationService evaluationService) {
		this.evaluationService = evaluationService;
	}

	@EventListener
	public void onPriceQuoteUpdated(PriceQuoteUpdatedEvent event) {
		evaluationService.evaluate(event.quote());
	}
}

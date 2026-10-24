package com.fxai.alert.service;

import com.fxai.alert.domain.PriceAlertEvaluator;
import com.fxai.alert.repository.PriceAlertRepository;
import com.fxai.market.PriceQuote;
import org.springframework.stereotype.Service;

@Service
public class PriceAlertEvaluationService {
	private final PriceAlertRepository repository;
	private final PriceAlertEvaluator evaluator = new PriceAlertEvaluator();

	public PriceAlertEvaluationService(PriceAlertRepository repository) {
		this.repository = repository;
	}

	public void evaluate(PriceQuote quote) {
		for (var alert : repository.findActiveBySymbol(quote.symbol())) {
			if (evaluator.isSatisfied(alert, quote.midpoint())) {
				repository.markTriggered(alert.id(), quote.timestamp());
			}
		}
	}
}

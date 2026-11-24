package com.fxai.alert.service;

import com.fxai.alert.api.AlertNotFoundException;
import com.fxai.alert.api.UnsupportedAlertSymbolException;
import com.fxai.alert.domain.AlertCondition;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.alert.repository.PriceAlertRepository;
import com.fxai.instrument.SupportedCurrencyPairs;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
public class PriceAlertService {
	private final PriceAlertRepository repository;
	private final Clock clock;

	@Autowired
	public PriceAlertService(PriceAlertRepository repository) {
		this(repository, Clock.systemUTC());
	}

	PriceAlertService(PriceAlertRepository repository, Clock clock) {
		this.repository = repository;
		this.clock = clock;
	}

	public PriceAlert create(UUID userId, String symbol, AlertCondition condition, BigDecimal targetPrice) {
		var pair = SupportedCurrencyPairs.findBySymbol(symbol)
				.orElseThrow(() -> new UnsupportedAlertSymbolException(symbol));
		return repository.saveForUser(PriceAlert.active(
				UUID.randomUUID(),
				pair.symbol(),
				condition,
				targetPrice,
				clock.instant()
		), userId);
	}

	public List<PriceAlert> findAll(UUID userId) {
		return repository.findAllByUserId(userId);
	}

	public void delete(UUID id, UUID userId) {
		if (!repository.deleteByIdAndUserId(id, userId)) {
			throw new AlertNotFoundException(id);
		}
	}
}

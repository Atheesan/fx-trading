package com.fxai.alert.repository;

import com.fxai.alert.domain.PriceAlert;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PriceAlertRepository {
	PriceAlert save(PriceAlert alert);

	PriceAlert saveForUser(PriceAlert alert, UUID userId);

	List<PriceAlert> findAllByUserId(UUID userId);

	boolean deleteByIdAndUserId(UUID id, UUID userId);

	List<PriceAlert> findAll();

	List<PriceAlert> findActiveBySymbol(String symbol);

	Optional<PriceAlert> markTriggered(UUID id, Instant triggeredAt);

	boolean deleteById(UUID id);
}

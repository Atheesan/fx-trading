package com.fxai.alert.persistence;

import com.fxai.alert.domain.AlertStatus;
import com.fxai.alert.domain.PriceAlert;
import com.fxai.alert.repository.PriceAlertRepository;
import com.fxai.instrument.persistence.CurrencyPairEntity;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@Transactional
public class JpaPriceAlertRepository implements PriceAlertRepository {
	private final PriceAlertJpaRepository repository;
	private final EntityManager entityManager;

	public JpaPriceAlertRepository(PriceAlertJpaRepository repository, EntityManager entityManager) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public PriceAlert save(PriceAlert alert) {
		CurrencyPairEntity pair = entityManager.getReference(CurrencyPairEntity.class, alert.symbol());
		return repository.save(new PriceAlertEntity(alert, pair)).toDomain();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PriceAlert> findAll() {
		return repository.findAllByOrderByCreatedAtDesc().stream()
				.map(PriceAlertEntity::toDomain)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<PriceAlert> findActiveBySymbol(String symbol) {
		return repository.findByCurrencyPair_SymbolAndStatusOrderByCreatedAtAsc(symbol, AlertStatus.ACTIVE).stream()
				.map(PriceAlertEntity::toDomain)
				.toList();
	}

	@Override
	public Optional<PriceAlert> markTriggered(UUID id, Instant triggeredAt) {
		int updated = repository.markTriggered(
				id,
				triggeredAt,
				AlertStatus.ACTIVE,
				AlertStatus.TRIGGERED
		);
		return updated == 0 ? Optional.empty() : repository.findById(id).map(PriceAlertEntity::toDomain);
	}

	@Override
	public boolean deleteById(UUID id) {
		return repository.deleteAlertById(id) == 1;
	}
}

package com.fxai.alert.repository;

import com.fxai.alert.domain.AlertStatus;
import com.fxai.alert.domain.PriceAlert;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class InMemoryPriceAlertRepository implements PriceAlertRepository {
	private final ConcurrentHashMap<UUID, PriceAlert> alerts = new ConcurrentHashMap<>();

	private final ConcurrentHashMap<UUID, UUID> owners = new ConcurrentHashMap<>();

    @Override
    public PriceAlert saveForUser(PriceAlert alert, UUID userId) { owners.put(alert.id(), userId); return save(alert); }

    @Override
    public List<PriceAlert> findAllByUserId(UUID userId) { return findAll().stream().filter(a -> userId.equals(owners.get(a.id()))).toList(); }

    @Override
    public boolean deleteByIdAndUserId(UUID id, UUID userId) { return userId.equals(owners.get(id)) && deleteById(id); }

	@Override
	public PriceAlert save(PriceAlert alert) {
		alerts.put(alert.id(), alert);
		return alert;
	}

	@Override
	public List<PriceAlert> findAll() {
		return alerts.values().stream()
				.sorted(Comparator.comparing(PriceAlert::createdAt).reversed())
				.toList();
	}

	@Override
	public List<PriceAlert> findActiveBySymbol(String symbol) {
		return alerts.values().stream()
				.filter(alert -> alert.status() == AlertStatus.ACTIVE)
				.filter(alert -> alert.symbol().equals(symbol))
				.sorted(Comparator.comparing(PriceAlert::createdAt))
				.toList();
	}

	@Override
	public Optional<PriceAlert> markTriggered(UUID id, Instant triggeredAt) {
		var triggered = new AtomicReference<PriceAlert>();
		alerts.computeIfPresent(id, (ignored, current) -> {
			if (current.status() != AlertStatus.ACTIVE) {
				return current;
			}
			PriceAlert updated = current.trigger(triggeredAt);
			triggered.set(updated);
			return updated;
		});
		return Optional.ofNullable(triggered.get());
	}

	@Override
	public boolean deleteById(UUID id) {
		return alerts.remove(id) != null;
	}
}

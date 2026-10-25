package com.fxai.market.persistence;

import com.fxai.instrument.persistence.CurrencyPairEntity;
import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.market.repository.HistoricalCandleRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
@Transactional
public class JpaHistoricalCandleRepository implements HistoricalCandleRepository {
	private final HistoricalCandleJpaRepository repository;
	private final EntityManager entityManager;

	public JpaHistoricalCandleRepository(
			HistoricalCandleJpaRepository repository,
			EntityManager entityManager
	) {
		this.repository = repository;
		this.entityManager = entityManager;
	}

	@Override
	public void saveAll(List<Candle> candles) {
		List<HistoricalCandleEntity> entities = candles.stream()
				.map(candle -> new HistoricalCandleEntity(
						candle,
						entityManager.getReference(CurrencyPairEntity.class, candle.symbol())
				))
				.toList();
		repository.saveAll(entities);
	}

	@Override
	@Transactional(readOnly = true)
	public List<Candle> find(String symbol, CandleQuery query) {
		Specification<HistoricalCandleEntity> specification = (root, criteriaQuery, builder) -> {
			var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
			predicates.add(builder.equal(root.get("id").get("currencyPairSymbol"), symbol));
			predicates.add(builder.equal(root.get("id").get("timeframe"), query.timeframe().code()));
			if (query.from() != null) {
				predicates.add(builder.greaterThanOrEqualTo(root.get("id").get("timestamp"), query.from()));
			}
			if (query.to() != null) {
				predicates.add(builder.lessThanOrEqualTo(root.get("id").get("timestamp"), query.to()));
			}
			return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
		};

		var page = PageRequest.of(
				0,
				query.limit(),
				Sort.by(Sort.Direction.ASC, "id.timestamp")
		);
		return repository.findAll(specification, page).stream()
				.map(HistoricalCandleEntity::toDomain)
				.toList();
	}
}

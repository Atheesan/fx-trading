package com.fxai.market.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface HistoricalCandleJpaRepository extends
		JpaRepository<HistoricalCandleEntity, HistoricalCandleId>,
		JpaSpecificationExecutor<HistoricalCandleEntity> {
}

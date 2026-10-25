package com.fxai.alert.persistence;

import com.fxai.alert.domain.AlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface PriceAlertJpaRepository extends JpaRepository<PriceAlertEntity, UUID> {
	List<PriceAlertEntity> findAllByOrderByCreatedAtDesc();

	List<PriceAlertEntity> findByCurrencyPair_SymbolAndStatusOrderByCreatedAtAsc(
			String symbol,
			AlertStatus status
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update PriceAlertEntity alert
			set alert.status = :triggeredStatus, alert.triggeredAt = :triggeredAt
			where alert.id = :id and alert.status = :activeStatus
			""")
	int markTriggered(
			@Param("id") UUID id,
			@Param("triggeredAt") Instant triggeredAt,
			@Param("activeStatus") AlertStatus activeStatus,
			@Param("triggeredStatus") AlertStatus triggeredStatus
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("delete from PriceAlertEntity alert where alert.id = :id")
	int deleteAlertById(@Param("id") UUID id);
}

package com.fxai.instrument.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyPairJpaRepository extends JpaRepository<CurrencyPairEntity, String> {
}

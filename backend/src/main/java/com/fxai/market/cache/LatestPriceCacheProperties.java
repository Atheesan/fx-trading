package com.fxai.market.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("fxai.market.latest-price-cache")
public record LatestPriceCacheProperties(Duration ttl) {
	public LatestPriceCacheProperties {
		if (ttl == null || ttl.isZero() || ttl.isNegative()) {
			throw new IllegalArgumentException("fxai.market.latest-price-cache.ttl must be positive");
		}
	}
}

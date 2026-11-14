package com.fxai.market.cache;

import com.fxai.PostgresIntegrationTest;
import com.fxai.market.PriceQuote;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RedisLatestPriceCacheIntegrationTests extends PostgresIntegrationTest {
	private static final GenericContainer<?> REDIS = new GenericContainer<>(
			DockerImageName.parse("redis:7.4-alpine")
	).withExposedPorts(6379);

	static {
		REDIS.start();
	}

	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		// Allow a freshly started Docker container time to establish its first connection.
		registry.add("spring.data.redis.connect-timeout", () -> "5s");
		registry.add("spring.data.redis.timeout", () -> "5s");
		registry.add("fxai.market.latest-price-cache.ttl", () -> "3s");
	}

	@Autowired
	private LatestPriceCache latestPriceCache;

	@Autowired
	private StringRedisTemplate redisTemplate;

	@Test
	void storesPredictableJsonUnderSymbolKeyWithConfiguredTtl() {
		PriceQuote quote = PriceQuote.fromBidAndAsk(
				"EURUSD",
				new BigDecimal("1.08420"),
				new BigDecimal("1.08440"),
				Instant.parse("2026-01-02T03:04:05Z")
		);

		latestPriceCache.put(quote);

		assertThat(latestPriceCache.get("EURUSD")).contains(quote);
		assertThat(redisTemplate.opsForValue().get("fxai:price:EURUSD"))
				.isEqualTo("{\"symbol\":\"EURUSD\",\"bid\":1.08420,\"ask\":1.08440,"
						+ "\"midpoint\":1.08430,\"spread\":0.00020,"
						+ "\"timestamp\":\"2026-01-02T03:04:05Z\"}");
		assertThat(redisTemplate.getExpire("fxai:price:EURUSD"))
				.isBetween(1L, 3L);
	}
}

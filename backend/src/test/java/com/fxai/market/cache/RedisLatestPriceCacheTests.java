package com.fxai.market.cache;

import com.fxai.market.PriceQuote;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLatestPriceCacheTests {
	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ObjectMapper objectMapper;

	@Test
	void treatsRedisReadFailureAsCacheMiss() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		when(valueOperations.get("fxai:price:EURUSD"))
				.thenThrow(new IllegalStateException("Redis unavailable"));
		var cache = cache();

		assertThat(cache.get("EURUSD")).isEmpty();
	}

	@Test
	void ignoresRedisWriteFailure() {
		PriceQuote quote = quote();
		when(objectMapper.writeValueAsString(quote)).thenReturn("{\"symbol\":\"EURUSD\"}");
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		doThrow(new IllegalStateException("Redis unavailable"))
				.when(valueOperations)
				.set("fxai:price:EURUSD", "{\"symbol\":\"EURUSD\"}", Duration.ofSeconds(5));
		var cache = cache();

		assertThatCode(() -> cache.put(quote)).doesNotThrowAnyException();
	}

	private RedisLatestPriceCache cache() {
		return new RedisLatestPriceCache(
				redisTemplate,
				objectMapper,
				new LatestPriceCacheProperties(Duration.ofSeconds(5))
		);
	}

	private PriceQuote quote() {
		return PriceQuote.fromBidAndAsk(
				"EURUSD",
				new BigDecimal("1.08420"),
				new BigDecimal("1.08440"),
				Instant.parse("2026-01-02T03:04:05Z")
		);
	}
}

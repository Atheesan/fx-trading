package com.fxai.market.cache;

import com.fxai.market.PriceQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;

@Component
public class RedisLatestPriceCache implements LatestPriceCache {
	static final String KEY_PREFIX = "fxai:price:";

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisLatestPriceCache.class);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;
	private final LatestPriceCacheProperties properties;

	public RedisLatestPriceCache(
			StringRedisTemplate redisTemplate,
			ObjectMapper objectMapper,
			LatestPriceCacheProperties properties
	) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
		this.properties = properties;
	}

	@Override
	public Optional<PriceQuote> get(String symbol) {
		String key = key(symbol);
		try {
			String json = redisTemplate.opsForValue().get(key);
			if (json == null) {
				return Optional.empty();
			}

			PriceQuote quote = objectMapper.readValue(json, PriceQuote.class);
			if (!symbol.equals(quote.symbol())) {
				LOGGER.atWarn()
						.addKeyValue("cache_key", key)
						.log("Ignoring latest price cache entry with a mismatched symbol");
				return Optional.empty();
			}
			return Optional.of(quote);
		} catch (RuntimeException exception) {
			logFailure("read", key, exception);
			return Optional.empty();
		}
	}

	@Override
	public void put(PriceQuote quote) {
		String key = key(quote.symbol());
		try {
			String json = objectMapper.writeValueAsString(quote);
			redisTemplate.opsForValue().set(key, json, properties.ttl());
		} catch (RuntimeException exception) {
			logFailure("write", key, exception);
		}
	}

	private String key(String symbol) {
		return KEY_PREFIX + symbol;
	}

	private void logFailure(String operation, String key, Exception exception) {
		LOGGER.atWarn()
				.addKeyValue("operation", operation)
				.addKeyValue("cache_key", key)
				.addKeyValue("error_type", exception.getClass().getSimpleName())
				.log("Latest price cache operation failed; continuing without Redis");
	}
}

package com.fxai.market.cache;

import com.fxai.market.PriceQuote;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Optional;
import java.util.List;

@Component
public class RedisLatestPriceCache implements LatestPriceCache {
	static final String KEY_PREFIX = "fxai:price:";

	private static final Logger LOGGER = LoggerFactory.getLogger(RedisLatestPriceCache.class);
	private static final DefaultRedisScript<Long> PUT_IF_NEWER = new DefaultRedisScript<>("""
			local function normalized(timestamp)
			  local base, fraction = string.match(timestamp, '^(.-)%.(%d+)Z$')
			  if not base then base = string.gsub(timestamp, 'Z$', ''); fraction = '' end
			  return base .. '.' .. string.sub(fraction .. '000000000', 1, 9)
			end
			local previous = redis.call('GET', KEYS[1])
			if previous then
			  local ok, value = pcall(cjson.decode, previous)
			  if ok and type(value) == 'table' and type(value.timestamp) == 'string'
			      and normalized(value.timestamp) >= normalized(ARGV[2]) then
			    return 0
			  end
			end
			redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[3])
			return 1
			""", Long.class);

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
			putIfNewer(quote);
		} catch (RuntimeException exception) {
			logFailure("write", key, exception);
		}
	}

	/** Kafka writes propagate failures so listener retries apply; REST writes remain best effort. */
	public void putIfNewer(PriceQuote quote) {
		redisTemplate.execute(PUT_IF_NEWER, List.of(key(quote.symbol())),
				objectMapper.writeValueAsString(quote), quote.timestamp().toString(),
				Long.toString(Math.max(1, properties.ttl().toMillis())));
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

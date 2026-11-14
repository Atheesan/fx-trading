package com.fxai.market.messaging;

import com.fxai.market.cache.RedisLatestPriceCache;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RedisPriceTickConsumer {
	private final RedisLatestPriceCache cache;

	public RedisPriceTickConsumer(RedisLatestPriceCache cache) {
		this.cache = cache;
	}

	@KafkaListener(topics = PriceTickEvent.TOPIC, groupId = "${fxai.kafka.redis-group:fxai-latest-price-v1}")
	public void consume(ConsumerRecord<String, PriceTickEvent> record) {
		if (!record.value().symbol().equals(record.key())) {
			throw new IllegalArgumentException("Price tick key must match symbol");
		}
		cache.putIfNewer(record.value().toQuote());
	}
}

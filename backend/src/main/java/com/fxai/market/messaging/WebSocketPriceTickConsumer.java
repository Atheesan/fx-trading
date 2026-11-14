package com.fxai.market.messaging;

import com.fxai.market.realtime.PriceQuoteUpdatedEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class WebSocketPriceTickConsumer {
	private final ApplicationEventPublisher publisher;
	// Bounded by the supported symbol catalogue. Rebuilt after restart; clients refresh REST snapshots.
	private final Map<String, Instant> latest = new HashMap<>();

	public WebSocketPriceTickConsumer(ApplicationEventPublisher publisher) {
		this.publisher = publisher;
	}

	@KafkaListener(topics = PriceTickEvent.TOPIC, groupId = "${fxai.kafka.websocket-group:fxai-websocket-local-v1}",
			properties = "auto.offset.reset=latest")
	public synchronized void consume(ConsumerRecord<String, PriceTickEvent> record) {
		var event = record.value();
		if (!event.symbol().equals(record.key())) {
			throw new IllegalArgumentException("Price tick key must match symbol");
		}
		var previous = latest.get(event.symbol());
		if (previous != null && !event.timestamp().isAfter(previous)) {
			return;
		}
		// Existing WebSocket and alert listeners now run only after Kafka delivery.
		publisher.publishEvent(new PriceQuoteUpdatedEvent(event.toQuote()));
		latest.put(event.symbol(), event.timestamp());
	}
}

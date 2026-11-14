package com.fxai.market.messaging;

import java.util.concurrent.CompletionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class KafkaPriceTickPublisher implements PriceTickPublisher {
	private static final Logger LOGGER = LoggerFactory.getLogger(KafkaPriceTickPublisher.class);
	private final KafkaTemplate<String, PriceTickEvent> template;

	public KafkaPriceTickPublisher(KafkaTemplate<String, PriceTickEvent> template) {
		this.template = template;
	}

	@Override
	public void publish(PriceTickEvent event) {
		try {
			// Wait for broker acknowledgement: bounded by producer delivery.timeout.ms.
			var result = template.send(PriceTickEvent.TOPIC, event.symbol(), event).join();
			LOGGER.atDebug().addKeyValue("event_id", event.eventId()).addKeyValue("symbol", event.symbol())
					.addKeyValue("partition", result.getRecordMetadata().partition()).log("Published price tick");
		} catch (CompletionException exception) {
			throw new IllegalStateException("Kafka price tick publication failed", exception.getCause());
		}
	}
}

package com.fxai.market.messaging;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class KafkaPriceTickPublisherTests {
	@Test
	@SuppressWarnings("unchecked")
	void usesSymbolKeyAndReportsBrokerFailure() {
		KafkaTemplate<String, PriceTickEvent> template = mock(KafkaTemplate.class);
		var event = PriceTickConsumerTests.event(Instant.now());
		when(template.send(PriceTickEvent.TOPIC, "EURUSD", event)).thenReturn(
				CompletableFuture.failedFuture(new IllegalStateException("Broker unavailable")));
		assertThatIllegalStateException().isThrownBy(() -> new KafkaPriceTickPublisher(template).publish(event));
		verify(template).send(PriceTickEvent.TOPIC, "EURUSD", event);
	}
}

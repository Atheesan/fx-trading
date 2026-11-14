package com.fxai.market.messaging;

import com.fxai.market.PriceQuote;
import com.fxai.market.cache.RedisLatestPriceCache;
import com.fxai.market.realtime.PriceQuoteUpdatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PriceTickConsumerTests {
	static PriceTickEvent event(Instant timestamp) {
		return PriceTickEvent.from(PriceQuote.fromBidAndAsk("EURUSD", new BigDecimal("1.10000"),
				new BigDecimal("1.10020"), timestamp), "mock");
	}

	static ConsumerRecord<String, PriceTickEvent> record(PriceTickEvent event) {
		return new ConsumerRecord<>(PriceTickEvent.TOPIC, 0, 0, event.symbol(), event);
	}

	@Test
	void jsonRoundTripPreservesDecimalPrecisionAndUtcTimestamp() {
		var mapper = JsonMapper.builder().build();
		var event = event(Instant.parse("2026-01-01T00:00:00.123456789Z"));
		String json = mapper.writeValueAsString(event);
		assertThat(mapper.readValue(json, PriceTickEvent.class)).isEqualTo(event);
		assertThat(mapper.readTree(json).properties()).hasSize(10);
		assertThat(json).contains("\"mid\":1.10010", "2026-01-01T00:00:00.123456789Z");
	}

	@Test
	void rejectsUnsupportedVersionAndInconsistentPrices() {
		var event = event(Instant.now());
		assertThatIllegalArgumentException().isThrownBy(() -> new PriceTickEvent(event.eventId(),
				event.eventType(), 2, event.symbol(), event.bid(), event.ask(), event.mid(),
				event.spread(), event.timestamp(), event.source()));
		assertThatIllegalArgumentException().isThrownBy(() -> new PriceTickEvent(event.eventId(),
				event.eventType(), 1, event.symbol(), event.bid(), event.ask(), BigDecimal.ZERO,
				event.spread(), event.timestamp(), event.source()));
	}

	@Test
	void websocketSuppressesDuplicatesAndOlderTicksButAcceptsNewerTicks() {
		var publisher = mock(ApplicationEventPublisher.class);
		var consumer = new WebSocketPriceTickConsumer(publisher);
		var event = event(Instant.parse("2026-01-01T00:00:01Z"));
		consumer.consume(record(event));
		consumer.consume(record(event));
		consumer.consume(record(event(event.timestamp().minusSeconds(1))));
		consumer.consume(record(event(event.timestamp().plusSeconds(1))));
		verify(publisher, times(2)).publishEvent(any(PriceQuoteUpdatedEvent.class));
	}

	@Test
	void redisFailurePropagatesForKafkaRetryAndKeyMismatchIsRejected() {
		var cache = mock(RedisLatestPriceCache.class);
		var consumer = new RedisPriceTickConsumer(cache);
		var event = event(Instant.now());
		doThrow(new IllegalStateException("Redis unavailable")).when(cache).putIfNewer(event.toQuote());
		assertThatIllegalStateException().isThrownBy(() -> consumer.consume(record(event)));
		assertThatIllegalArgumentException().isThrownBy(() -> consumer.consume(
				new ConsumerRecord<>(PriceTickEvent.TOPIC, 0, 0, "GBPUSD", event)));
	}

	@Test
	void failedDeliveryCanBeRetried() {
		var publisher = mock(ApplicationEventPublisher.class);
		var consumer = new WebSocketPriceTickConsumer(publisher);
		var event = event(Instant.now());
		doThrow(new IllegalStateException("Listener failed")).doNothing()
				.when(publisher).publishEvent(any(PriceQuoteUpdatedEvent.class));
		assertThatIllegalStateException().isThrownBy(() -> consumer.consume(record(event)));
		consumer.consume(record(event));
		verify(publisher, times(2)).publishEvent(any(PriceQuoteUpdatedEvent.class));
	}
}

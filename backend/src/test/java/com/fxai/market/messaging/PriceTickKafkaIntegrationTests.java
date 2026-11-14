package com.fxai.market.messaging;

import com.fxai.market.cache.LatestPriceCacheConfiguration;
import com.fxai.market.cache.RedisLatestPriceCache;
import com.fxai.market.realtime.MarketPriceWebSocketHandler;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.web.socket.WebSocketSession;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.*;
import java.time.Duration;

/** Real KRaft broker, JSON serializers, independent groups, Redis Lua and WebSocket delivery. */
@SpringBootTest(classes = PriceTickKafkaIntegrationTests.TestConfiguration.class)
@ActiveProfiles("kafka-integration")
class PriceTickKafkaIntegrationTests {
	static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:4.1.2"));
	static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
			.withExposedPorts(6379);
	static {
		KAFKA.start();
		REDIS.start();
	}

	@Configuration(proxyBeanMethods = false)
	@Profile("kafka-integration")
	@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
	@Import({PriceTickKafkaConfiguration.class, KafkaPriceTickPublisher.class, RedisPriceTickConsumer.class,
			WebSocketPriceTickConsumer.class, RedisLatestPriceCache.class, LatestPriceCacheConfiguration.class,
			MarketPriceWebSocketHandler.class})
	static class TestConfiguration { }

	@DynamicPropertySource
	static void properties(DynamicPropertyRegistry registry) {
		registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
		registry.add("spring.data.redis.host", REDIS::getHost);
		registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
		registry.add("fxai.market.latest-price-cache.ttl", () -> "30s");
		registry.add("fxai.kafka.retry-interval", () -> "10");
	}

	@Autowired KafkaPriceTickPublisher publisher;
	@MockitoSpyBean RedisLatestPriceCache cache;
	@Autowired MarketPriceWebSocketHandler websocket;
	@Autowired KafkaListenerEndpointRegistry registry;

	@BeforeEach
	void waitForGroups() {
		registry.getListenerContainers().forEach(container -> ContainerTestUtils.waitForAssignment(container, 3));
	}

	@Test
	void bothGroupsReceiveTicksAndDuplicateOrOlderEventsDoNotRegressPrices() throws Exception {
		var session = mock(WebSocketSession.class);
		when(session.getId()).thenReturn("integration");
		when(session.isOpen()).thenReturn(true);
		websocket.afterConnectionEstablished(session);
		var tick = PriceTickConsumerTests.event(Instant.parse("2026-11-01T00:00:01.123456789Z"));
		publisher.publish(tick);
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(cache.get(tick.symbol())).contains(tick.toQuote()));
		verify(session, timeout(20000)).sendMessage(any());
		publisher.publish(tick);
		publisher.publish(PriceTickConsumerTests.event(tick.timestamp().minusSeconds(1)));
		var newer = PriceTickConsumerTests.event(tick.timestamp().plusSeconds(1));
		publisher.publish(newer);
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(cache.get(tick.symbol())).contains(newer.toQuote()));
		verify(session, timeout(20000).times(2)).sendMessage(any());
		// Shared guarded write also prevents a slow REST response overwriting a newer Kafka tick.
		cache.put(tick.toQuote());
		assertThat(cache.get(tick.symbol())).contains(newer.toQuote());

		// A malformed JSON record must not prevent delivery of the next valid tick.
		try (var raw = new org.apache.kafka.clients.producer.KafkaProducer<String, String>(java.util.Map.of(
				"bootstrap.servers", KAFKA.getBootstrapServers(),
				"key.serializer", "org.apache.kafka.common.serialization.StringSerializer",
				"value.serializer", "org.apache.kafka.common.serialization.StringSerializer"))) {
			raw.send(new org.apache.kafka.clients.producer.ProducerRecord<>(PriceTickEvent.TOPIC, "EURUSD", "{invalid"))
					.get(10, java.util.concurrent.TimeUnit.SECONDS);
		}
		var recovery = PriceTickConsumerTests.event(newer.timestamp().plusSeconds(1));
		doThrow(new org.springframework.data.redis.RedisConnectionFailureException("temporary outage"))
				.doCallRealMethod().when(cache).putIfNewer(recovery.toQuote());
		publisher.publish(recovery);
		await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(cache.get(tick.symbol())).contains(recovery.toQuote()));
		verify(cache, times(2)).putIfNewer(recovery.toQuote());
		verify(session, timeout(20000).times(3)).sendMessage(any());
		websocket.afterConnectionClosed(session, org.springframework.web.socket.CloseStatus.NORMAL);
	}
}

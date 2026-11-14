package com.fxai.market.messaging;

import org.apache.kafka.clients.admin.NewTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration(proxyBeanMethods = false)
public class PriceTickKafkaConfiguration {
	private static final Logger LOGGER = LoggerFactory.getLogger(PriceTickKafkaConfiguration.class);

	@Bean
	NewTopic priceTickTopic(@Value("${fxai.kafka.partitions:3}") int partitions,
			@Value("${fxai.kafka.replicas:1}") int replicas) {
		return TopicBuilder.name(PriceTickEvent.TOPIC).partitions(partitions).replicas(replicas)
				.config("retention.ms", "86400000").config("cleanup.policy", "delete").build();
	}

	@Bean
	DefaultErrorHandler priceTickErrorHandler(@Value("${fxai.kafka.retry-interval:1000}") long interval,
			@Value("${fxai.kafka.retry-attempts:3}") long attempts) {
		var handler = new DefaultErrorHandler((record, exception) -> {
			LOGGER.atError().addKeyValue("topic", record.topic()).addKeyValue("partition", record.partition())
					.addKeyValue("offset", record.offset()).addKeyValue("symbol", record.key())
					.addKeyValue("error_type", exception.getClass().getSimpleName())
					.log("Price tick discarded after failed delivery; replay requires an offset reset");
		}, new FixedBackOff(interval, attempts));
		handler.addNotRetryableExceptions(IllegalArgumentException.class);
		handler.setRetryListeners((record, exception, attempt) -> LOGGER.atWarn()
				.addKeyValue("topic", record.topic()).addKeyValue("partition", record.partition())
				.addKeyValue("offset", record.offset()).addKeyValue("attempt", attempt)
				.addKeyValue("error_type", exception.getClass().getSimpleName()).log("Price tick delivery failed"));
		return handler;
	}
}

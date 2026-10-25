package com.fxai;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = "fxai.market.price-poll-interval=3600000")
public abstract class PostgresIntegrationTest {
	private static final DockerImageName TIMESCALE_IMAGE = DockerImageName
			.parse("timescale/timescaledb:2.17.2-pg17")
			.asCompatibleSubstituteFor("postgres");

	static final PostgreSQLContainer<?> DATABASE = new PostgreSQLContainer<>(TIMESCALE_IMAGE)
			.withDatabaseName("fxai_test")
			.withUsername("fxai_test")
			.withPassword("fxai_test_password");

	static {
		DATABASE.start();
	}

	@DynamicPropertySource
	static void databaseProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", DATABASE::getJdbcUrl);
		registry.add("spring.datasource.username", DATABASE::getUsername);
		registry.add("spring.datasource.password", DATABASE::getPassword);
	}
}

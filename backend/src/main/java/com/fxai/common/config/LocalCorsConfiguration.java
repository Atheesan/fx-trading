package com.fxai.common.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
@Profile("local")
@EnableConfigurationProperties(LocalCorsProperties.class)
public class LocalCorsConfiguration implements WebMvcConfigurer {

	private final LocalCorsProperties properties;

	public LocalCorsConfiguration(LocalCorsProperties properties) {
		this.properties = properties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOrigins(properties.allowedOrigins().toArray(String[]::new))
				.allowedMethods("GET", "HEAD", "OPTIONS", "POST", "PUT", "DELETE")
				.allowCredentials(true)
				.allowedHeaders("*")
				.maxAge(3600);
	}

}

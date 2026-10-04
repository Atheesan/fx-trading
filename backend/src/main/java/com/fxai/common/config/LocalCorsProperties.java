package com.fxai.common.config;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("fxai.cors")
public record LocalCorsProperties(
		@NotEmpty List<@NotBlank String> allowedOrigins
) {
}

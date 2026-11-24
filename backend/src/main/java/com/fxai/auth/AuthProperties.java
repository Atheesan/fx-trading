package com.fxai.auth;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("fxai.auth")
public record AuthProperties(@NotBlank String signingKey, boolean secureCookie) {
}

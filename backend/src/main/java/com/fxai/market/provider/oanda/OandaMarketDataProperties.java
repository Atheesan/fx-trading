package com.fxai.market.provider.oanda;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fxai.market.oanda")
public record OandaMarketDataProperties(
        URI baseUrl,
        String apiToken,
        String accountId,
        Duration connectionTimeout,
        Duration requestTimeout) {

    public OandaMarketDataProperties {
        if (baseUrl == null) baseUrl = URI.create("https://api-fxpractice.oanda.com");
        if (connectionTimeout == null) connectionTimeout = Duration.ofSeconds(2);
        if (requestTimeout == null) requestTimeout = Duration.ofSeconds(5);
    }

    void validate() {
        if (apiToken == null || apiToken.isBlank())
            throw new IllegalStateException("OANDA_API_TOKEN is required when the OANDA provider is selected");
        if (accountId == null || accountId.isBlank())
            throw new IllegalStateException("OANDA_ACCOUNT_ID is required when the OANDA provider is selected");
        if (connectionTimeout.isZero() || connectionTimeout.isNegative())
            throw new IllegalStateException("OANDA connection timeout must be positive");
        if (requestTimeout.isZero() || requestTimeout.isNegative())
            throw new IllegalStateException("OANDA request timeout must be positive");
    }
}

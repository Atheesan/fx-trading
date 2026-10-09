package com.fxai.market.provider.oanda;

import java.net.http.HttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "fxai.market.provider", havingValue = "oanda")
@EnableConfigurationProperties(OandaMarketDataProperties.class)
public class OandaMarketDataConfiguration {
    @Bean
    RestClient oandaRestClient(OandaMarketDataProperties properties) {
        properties.validate();
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectionTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiToken())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("Accept-Datetime-Format", "RFC3339")
                .build();
    }

    @Bean
    OandaRestApiClient oandaRestApiClient(RestClient oandaRestClient) {
        return new OandaRestApiClient(oandaRestClient);
    }
}

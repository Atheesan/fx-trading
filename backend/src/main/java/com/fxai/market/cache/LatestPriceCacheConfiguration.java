package com.fxai.market.cache;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LatestPriceCacheProperties.class)
public class LatestPriceCacheConfiguration {
}

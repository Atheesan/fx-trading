package com.fxai.market.realtime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
@EnableWebSocket
public class MarketWebSocketConfiguration implements WebSocketConfigurer {

	private final MarketPriceWebSocketHandler handler;
	private final String allowedOrigin;

	public MarketWebSocketConfiguration(
			MarketPriceWebSocketHandler handler,
			@Value("${fxai.websocket.allowed-origin:http://localhost:5173}") String allowedOrigin
	) {
		this.handler = handler;
		this.allowedOrigin = allowedOrigin;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler, "/ws/market")
				.setAllowedOrigins(allowedOrigin);
	}

}

package com.fxai.market.realtime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fxai.market.PriceQuote;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class MarketPriceWebSocketHandlerTests {

	@Mock
	private WebSocketSession session;

	private ObjectMapper objectMapper;
	private MarketPriceWebSocketHandler handler;

	@BeforeEach
	void setUp() {
		objectMapper = JsonMapper.builder().findAndAddModules().build();
		handler = new MarketPriceWebSocketHandler(objectMapper);
		when(session.getId()).thenReturn("session-1");
	}

	@Test
	void broadcastsTheRequiredPriceUpdateMessage() throws Exception {
		when(session.isOpen()).thenReturn(true);
		handler.afterConnectionEstablished(session);

		handler.broadcast(new PriceQuoteUpdatedEvent(PriceQuote.fromBidAndAsk(
				"EURUSD",
				new BigDecimal("1.1000"),
				new BigDecimal("1.1002"),
				Instant.parse("2026-01-02T03:04:05Z")
		)));

		ArgumentCaptor<TextMessage> message = ArgumentCaptor.forClass(TextMessage.class);
		verify(session).sendMessage(message.capture());
		JsonNode json = objectMapper.readTree(message.getValue().getPayload());
		assertThat(json.get("type").asText()).isEqualTo("PRICE_UPDATE");
		assertThat(json.get("symbol").asText()).isEqualTo("EURUSD");
		assertThat(json.get("bid").decimalValue()).isEqualByComparingTo("1.1000");
		assertThat(json.get("ask").decimalValue()).isEqualByComparingTo("1.1002");
		assertThat(json.get("mid").decimalValue()).isEqualByComparingTo("1.1001");
		assertThat(json.get("spread").decimalValue()).isEqualByComparingTo("0.0002");
		assertThat(json.get("timestamp").asText()).isEqualTo("2026-01-02T03:04:05Z");
	}

	@Test
	void removesClosedConnections() throws Exception {
		handler.afterConnectionEstablished(session);
		assertThat(handler.connectionCount()).isOne();

		handler.afterConnectionClosed(session, CloseStatus.NORMAL);

		assertThat(handler.connectionCount()).isZero();
	}

}

package com.fxai.market.realtime;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class MarketPriceWebSocketHandler extends TextWebSocketHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MarketPriceWebSocketHandler.class);

	private final ObjectMapper objectMapper;
	private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

	public MarketPriceWebSocketHandler(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		sessions.put(session.getId(), session);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		sessions.remove(session.getId());
	}

	@Override
	public void handleTransportError(WebSocketSession session, Throwable exception) {
		sessions.remove(session.getId());
		closeQuietly(session);
	}

	@EventListener
	public void broadcast(PriceQuoteUpdatedEvent event) {
		TextMessage message;
		try {
			message = new TextMessage(objectMapper.writeValueAsString(PriceUpdateMessage.from(event.quote())));
		} catch (JacksonException exception) {
			LOGGER.error("Could not serialize market price update", exception);
			return;
		}

		for (Map.Entry<String, WebSocketSession> entry : sessions.entrySet()) {
			WebSocketSession session = entry.getValue();
			if (!session.isOpen()) {
				sessions.remove(entry.getKey(), session);
				continue;
			}
			try {
				synchronized (session) {
					session.sendMessage(message);
				}
			} catch (IOException exception) {
				sessions.remove(entry.getKey(), session);
				closeQuietly(session);
				LOGGER.atWarn()
						.addKeyValue("session_id", session.getId())
						.log("Could not send market price update");
			}
		}
	}

	int connectionCount() {
		return sessions.size();
	}

	private void closeQuietly(WebSocketSession session) {
		try {
			if (session.isOpen()) {
				session.close(CloseStatus.SERVER_ERROR);
			}
		} catch (IOException exception) {
			LOGGER.debug("Could not close failed WebSocket session", exception);
		}
	}

}

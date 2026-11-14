package com.fxai.market.messaging;

public interface PriceTickPublisher {
	void publish(PriceTickEvent event);
}

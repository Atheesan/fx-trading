package com.fxai.market.repository;

import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;

import java.util.List;

public interface HistoricalCandleRepository {
	void saveAll(List<Candle> candles);

	List<Candle> find(String symbol, CandleQuery query);
}

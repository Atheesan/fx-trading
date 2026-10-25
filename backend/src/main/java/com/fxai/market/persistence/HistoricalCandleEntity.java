package com.fxai.market.persistence;

import com.fxai.instrument.persistence.CurrencyPairEntity;
import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleTimeframe;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "historical_candle")
public class HistoricalCandleEntity {
	@EmbeddedId
	private HistoricalCandleId id;

	@MapsId("currencyPairSymbol")
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "currency_pair_symbol", nullable = false)
	private CurrencyPairEntity currencyPair;

	@Column(name = "open_price", precision = 19, scale = 10, nullable = false)
	private BigDecimal open;

	@Column(name = "high_price", precision = 19, scale = 10, nullable = false)
	private BigDecimal high;

	@Column(name = "low_price", precision = 19, scale = 10, nullable = false)
	private BigDecimal low;

	@Column(name = "close_price", precision = 19, scale = 10, nullable = false)
	private BigDecimal close;

	private Long volume;

	protected HistoricalCandleEntity() {
	}

	HistoricalCandleEntity(Candle candle, CurrencyPairEntity currencyPair) {
		this.id = new HistoricalCandleId(
				candle.symbol(),
				candle.timeframe().code(),
				candle.timestamp()
		);
		this.currencyPair = currencyPair;
		this.open = candle.open();
		this.high = candle.high();
		this.low = candle.low();
		this.close = candle.close();
		this.volume = candle.volume();
	}

	public Candle toDomain() {
		return new Candle(
				currencyPair.getSymbol(),
				CandleTimeframe.fromCode(id.timeframe()).orElseThrow(),
				id.timestamp(),
				open,
				high,
				low,
				close,
				volume
		);
	}
}

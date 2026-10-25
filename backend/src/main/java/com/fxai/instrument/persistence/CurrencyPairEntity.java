package com.fxai.instrument.persistence;

import com.fxai.instrument.Currency;
import com.fxai.instrument.CurrencyPair;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "currency_pair")
public class CurrencyPairEntity {
	@Id
	@Column(length = 6, nullable = false)
	private String symbol;

	@Column(name = "base_currency", length = 3, nullable = false)
	private String baseCurrency;

	@Column(name = "quote_currency", length = 3, nullable = false)
	private String quoteCurrency;

	@Column(name = "display_name", length = 7, nullable = false)
	private String displayName;

	protected CurrencyPairEntity() {
	}

	public CurrencyPairEntity(CurrencyPair pair) {
		this.symbol = pair.symbol();
		this.baseCurrency = pair.baseCurrency().code();
		this.quoteCurrency = pair.quoteCurrency().code();
		this.displayName = pair.displayName();
	}

	public String getSymbol() {
		return symbol;
	}

	public CurrencyPair toDomain() {
		return new CurrencyPair(
				Currency.valueOf(baseCurrency),
				Currency.valueOf(quoteCurrency),
				symbol,
				displayName
		);
	}
}

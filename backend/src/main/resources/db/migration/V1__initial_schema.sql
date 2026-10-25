CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE currency_pair (
    symbol VARCHAR(6) PRIMARY KEY,
    base_currency VARCHAR(3) NOT NULL,
    quote_currency VARCHAR(3) NOT NULL,
    display_name VARCHAR(7) NOT NULL,
    CONSTRAINT currency_pair_distinct_currencies CHECK (base_currency <> quote_currency)
);

INSERT INTO currency_pair (symbol, base_currency, quote_currency, display_name) VALUES
    ('EURUSD', 'EUR', 'USD', 'EUR/USD'),
    ('GBPUSD', 'GBP', 'USD', 'GBP/USD'),
    ('USDJPY', 'USD', 'JPY', 'USD/JPY'),
    ('AUDUSD', 'AUD', 'USD', 'AUD/USD'),
    ('NZDUSD', 'NZD', 'USD', 'NZD/USD'),
    ('USDCAD', 'USD', 'CAD', 'USD/CAD'),
    ('USDCHF', 'USD', 'CHF', 'USD/CHF'),
    ('EURGBP', 'EUR', 'GBP', 'EUR/GBP');

CREATE TABLE price_alert (
    id UUID PRIMARY KEY,
    currency_pair_symbol VARCHAR(6) NOT NULL REFERENCES currency_pair (symbol),
    condition VARCHAR(20) NOT NULL,
    target_price NUMERIC(19, 8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    triggered_at TIMESTAMPTZ,
    CONSTRAINT price_alert_positive_target CHECK (target_price > 0),
    CONSTRAINT price_alert_trigger_state CHECK (
        (status = 'ACTIVE' AND triggered_at IS NULL)
        OR (status = 'TRIGGERED' AND triggered_at IS NOT NULL)
    )
);

CREATE INDEX price_alert_active_symbol_idx
    ON price_alert (currency_pair_symbol, created_at)
    WHERE status = 'ACTIVE';

CREATE TABLE historical_candle (
    currency_pair_symbol VARCHAR(6) NOT NULL REFERENCES currency_pair (symbol),
    timeframe VARCHAR(4) NOT NULL,
    candle_time TIMESTAMPTZ NOT NULL,
    open_price NUMERIC(19, 10) NOT NULL,
    high_price NUMERIC(19, 10) NOT NULL,
    low_price NUMERIC(19, 10) NOT NULL,
    close_price NUMERIC(19, 10) NOT NULL,
    volume BIGINT,
    PRIMARY KEY (currency_pair_symbol, timeframe, candle_time),
    CONSTRAINT historical_candle_non_negative_volume CHECK (volume IS NULL OR volume >= 0),
    CONSTRAINT historical_candle_valid_high CHECK (
        high_price >= open_price AND high_price >= close_price AND high_price >= low_price
    ),
    CONSTRAINT historical_candle_valid_low CHECK (
        low_price <= open_price AND low_price <= close_price
    )
);

SELECT create_hypertable(
    'historical_candle',
    'candle_time',
    if_not_exists => TRUE
);

CREATE INDEX historical_candle_lookup_idx
    ON historical_candle (currency_pair_symbol, timeframe, candle_time DESC);

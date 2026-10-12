package com.fxai.market.provider;

import com.fxai.market.domain.Candle;
import com.fxai.market.domain.CandleQuery;
import com.fxai.instrument.CurrencyPair;
import com.fxai.instrument.SupportedCurrencyPairs;
import com.fxai.market.PriceQuote;
import com.fxai.market.error.MarketDataAuthenticationException;
import com.fxai.market.error.MarketDataRateLimitException;
import com.fxai.market.error.MarketDataUnavailableException;
import com.fxai.market.provider.oanda.OandaMarketDataProperties;
import com.fxai.market.provider.oanda.OandaGranularityMapper;
import com.fxai.market.provider.oanda.OandaInstrumentMapper;
import com.fxai.market.provider.oanda.OandaRestApiClient;
import com.fxai.market.provider.oanda.dto.OandaCandleDto;
import com.fxai.market.provider.oanda.dto.OandaClientPriceDto;
import com.fxai.market.provider.oanda.exception.OandaAuthenticationException;
import com.fxai.market.provider.oanda.exception.OandaInvalidResponseException;
import com.fxai.market.provider.oanda.exception.OandaRateLimitException;
import com.fxai.market.provider.oanda.exception.OandaUnavailableException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(name = "fxai.market.provider", havingValue = "oanda")
public class OandaMarketDataProvider implements MarketDataProvider {
    private static final Logger LOGGER = LoggerFactory.getLogger(OandaMarketDataProvider.class);
    private final OandaRestApiClient client;
    private final OandaMarketDataProperties properties;

    public OandaMarketDataProvider(OandaRestApiClient client, OandaMarketDataProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public List<CurrencyPair> getCurrencyPairs() {
        return SupportedCurrencyPairs.all();
    }

    @Override
    public Optional<PriceQuote> getPrice(String symbol) {
        Optional<String> instrument = toOandaInstrument(symbol);
        if (instrument.isEmpty()) return Optional.empty();
        long started = System.nanoTime();
        try {
            OandaClientPriceDto price = client.fetchCurrentPrice(properties.accountId(), instrument.get()).prices().getFirst();
            validatePrice(price, instrument.get());
            BigDecimal bid = decimal(price.bids().getFirst().price());
            BigDecimal ask = decimal(price.asks().getFirst().price());
            PriceQuote quote = PriceQuote.fromBidAndAsk(
                    normalize(symbol), bid, ask, Instant.parse(price.time()));
            logSuccess("current-price", normalize(symbol), started);
            return Optional.of(quote);
        } catch (OandaAuthenticationException exception) {
            logFailure("current-price", symbol, exception, started);
            throw new MarketDataAuthenticationException("OANDA credentials were rejected");
        } catch (OandaRateLimitException exception) {
            logFailure("current-price", symbol, exception, started);
            throw new MarketDataRateLimitException("OANDA rate limit exceeded");
        } catch (OandaUnavailableException | OandaInvalidResponseException | IllegalArgumentException exception) {
            logFailure("current-price", symbol, exception, started);
            throw new MarketDataUnavailableException("Current market data is temporarily unavailable", exception);
        }
    }

    @Override
    public Optional<List<Candle>> getHistoricalCandles(String symbol, CandleQuery query) {
        Optional<String> instrument = toOandaInstrument(symbol);
        if (instrument.isEmpty()) return Optional.empty();
        long started = System.nanoTime();
        try {
            String granularity = OandaGranularityMapper.toOanda(query.timeframe());
            var response = client.fetchCandles(
                    instrument.get(), granularity, query.from(), query.to(), query.limit());
            if (!instrument.get().equals(response.instrument()) || !granularity.equals(response.granularity()))
                throw new OandaInvalidResponseException("OANDA candle response did not match the request");
            List<Candle> candles = response.candles().stream()
                    .limit(query.limit())
                    .map(candle -> mapCandle(normalize(symbol), query, candle))
                    .toList();
            logSuccess("historical-candles", normalize(symbol), started);
            return Optional.of(candles);
        } catch (OandaAuthenticationException exception) {
            logFailure("historical-candles", symbol, exception, started);
            throw new MarketDataAuthenticationException("OANDA credentials were rejected");
        } catch (OandaRateLimitException exception) {
            logFailure("historical-candles", symbol, exception, started);
            throw new MarketDataRateLimitException("OANDA rate limit exceeded");
        } catch (OandaUnavailableException | OandaInvalidResponseException | IllegalArgumentException exception) {
            logFailure("historical-candles", symbol, exception, started);
            throw new MarketDataUnavailableException("Historical market data is temporarily unavailable", exception);
        }
    }

    private Candle mapCandle(String symbol, CandleQuery query, OandaCandleDto candle) {
        if (candle == null || candle.mid() == null || candle.time() == null)
            throw new OandaInvalidResponseException("OANDA candle was missing required fields");
        return new Candle(symbol, query.timeframe(), Instant.parse(candle.time()),
                decimal(candle.mid().open()), decimal(candle.mid().high()),
                decimal(candle.mid().low()), decimal(candle.mid().close()), candle.volume());
    }

    private void validatePrice(OandaClientPriceDto price, String expectedInstrument) {
        if (price == null || !expectedInstrument.equals(price.instrument()) || price.time() == null
                || price.bids() == null || price.bids().isEmpty()
                || price.asks() == null || price.asks().isEmpty())
            throw new OandaInvalidResponseException("OANDA price was missing required fields");
    }

    private Optional<String> toOandaInstrument(String symbol) {
        String normalized = normalize(symbol);
        return SupportedCurrencyPairs.findBySymbol(normalized)
                .flatMap(pair -> OandaInstrumentMapper.toOanda(pair.symbol()));
    }

    private BigDecimal decimal(String value) {
        if (value == null || value.isBlank()) throw new OandaInvalidResponseException("OANDA price was blank");
        return new BigDecimal(value);
    }

    private String normalize(String symbol) {
        return symbol == null ? "" : symbol.replace("/", "").replace("_", "").toUpperCase(Locale.ROOT);
    }

    private void logSuccess(String operation, String symbol, long started) {
        LOGGER.atInfo().addKeyValue("provider", "oanda").addKeyValue("operation", operation)
                .addKeyValue("symbol", symbol).addKeyValue("duration_ms", elapsedMillis(started))
                .log("Market data request completed");
    }

    private void logFailure(String operation, String symbol, RuntimeException exception, long started) {
        LOGGER.atWarn().addKeyValue("provider", "oanda").addKeyValue("operation", operation)
                .addKeyValue("symbol", normalize(symbol)).addKeyValue("duration_ms", elapsedMillis(started))
                .addKeyValue("error_type", exception.getClass().getSimpleName())
                .log("Market data request failed");
    }

    private long elapsedMillis(long started) { return (System.nanoTime() - started) / 1_000_000; }
}

package com.fxai.market.provider.oanda;

import com.fxai.market.provider.oanda.dto.OandaCandlesResponse;
import com.fxai.market.provider.oanda.dto.OandaPricingResponse;
import com.fxai.market.provider.oanda.exception.OandaAuthenticationException;
import com.fxai.market.provider.oanda.exception.OandaInvalidResponseException;
import com.fxai.market.provider.oanda.exception.OandaRateLimitException;
import com.fxai.market.provider.oanda.exception.OandaUnavailableException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

public class OandaRestApiClient {
    private final RestClient restClient;

    OandaRestApiClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public OandaPricingResponse fetchCurrentPrice(String accountId, String instrument) {
        try {
            OandaPricingResponse response = restClient.get()
                    .uri("/v3/accounts/{accountId}/pricing?instruments={instrument}", accountId, instrument)
                    .retrieve()
                    .body(OandaPricingResponse.class);
            if (response == null || response.prices() == null || response.prices().isEmpty())
                throw new OandaInvalidResponseException("OANDA pricing response contained no prices");
            return response;
        } catch (OandaInvalidResponseException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw translateStatus(exception);
        } catch (ResourceAccessException exception) {
            throw new OandaUnavailableException("OANDA pricing request timed out or could not connect", exception);
        } catch (RestClientException exception) {
            throw new OandaInvalidResponseException("OANDA pricing response could not be processed", exception);
        }
    }

    public OandaCandlesResponse fetchCandles(String instrument, String granularity, int count) {
        try {
            OandaCandlesResponse response = restClient.get()
                    .uri("/v3/instruments/{instrument}/candles?price=M&granularity={granularity}&count={count}",
                            instrument, granularity, count)
                    .retrieve()
                    .body(OandaCandlesResponse.class);
            if (response == null || response.candles() == null)
                throw new OandaInvalidResponseException("OANDA candle response did not contain candles");
            return response;
        } catch (OandaInvalidResponseException exception) {
            throw exception;
        } catch (RestClientResponseException exception) {
            throw translateStatus(exception);
        } catch (ResourceAccessException exception) {
            throw new OandaUnavailableException("OANDA candle request timed out or could not connect", exception);
        } catch (RestClientException exception) {
            throw new OandaInvalidResponseException("OANDA candle response could not be processed", exception);
        }
    }

    private RuntimeException translateStatus(RestClientResponseException exception) {
        if (exception.getStatusCode() == HttpStatus.UNAUTHORIZED
                || exception.getStatusCode() == HttpStatus.FORBIDDEN)
            return new OandaAuthenticationException();
        if (exception.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS)
            return new OandaRateLimitException();
        return new OandaUnavailableException(
                "OANDA request failed with HTTP status " + exception.getStatusCode().value());
    }
}

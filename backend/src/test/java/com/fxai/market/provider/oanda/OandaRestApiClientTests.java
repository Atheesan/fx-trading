package com.fxai.market.provider.oanda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import com.fxai.market.provider.oanda.exception.OandaInvalidResponseException;
import com.fxai.market.provider.oanda.exception.OandaAuthenticationException;
import com.fxai.market.provider.oanda.exception.OandaRateLimitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OandaRestApiClientTests {
    private MockRestServiceServer server;
    private OandaRestApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder()
                .baseUrl("https://api-fxpractice.oanda.com")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer test-token");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new OandaRestApiClient(builder.build());
    }

    @Test
    void requestsCurrentPricingWithBearerAuthentication() {
        server.expect(requestTo("https://api-fxpractice.oanda.com/v3/accounts/test-account/pricing?instruments=EUR_USD"))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-token"))
                .andRespond(withSuccess("""
                        {"prices":[{"instrument":"EUR_USD","time":"2026-10-06T01:02:03Z",
                        "status":"tradeable","tradeable":true,"bids":[{"price":"1.1"}],
                        "asks":[{"price":"1.2"}]}]}
                        """, MediaType.APPLICATION_JSON));

        assertThat(client.fetchCurrentPrice("test-account", "EUR_USD").prices()).hasSize(1);
        server.verify();
    }

    @Test
    void requestsHistoricalMidpointCandles() {
        server.expect(requestTo("https://api-fxpractice.oanda.com/v3/instruments/EUR_USD/candles?price=M&granularity=H1&count=2"))
                .andRespond(withSuccess("""
                        {"instrument":"EUR_USD","granularity":"H1","candles":[]}
                        """, MediaType.APPLICATION_JSON));
        assertThat(client.fetchCandles("EUR_USD", "H1", 2).candles()).isEmpty();
        server.verify();
    }

    @Test
    void translatesRateLimitResponse() {
        server.expect(requestTo("https://api-fxpractice.oanda.com/v3/accounts/a/pricing?instruments=EUR_USD"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        assertThatThrownBy(() -> client.fetchCurrentPrice("a", "EUR_USD"))
                .isInstanceOf(OandaRateLimitException.class);
    }

    @Test
    void translatesAuthenticationFailure() {
        server.expect(requestTo("https://api-fxpractice.oanda.com/v3/accounts/a/pricing?instruments=EUR_USD"))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> client.fetchCurrentPrice("a", "EUR_USD"))
                .isInstanceOf(OandaAuthenticationException.class);
    }

    @Test
    void rejectsMalformedProviderPayload() {
        server.expect(requestTo("https://api-fxpractice.oanda.com/v3/accounts/a/pricing?instruments=EUR_USD"))
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> client.fetchCurrentPrice("a", "EUR_USD"))
                .isInstanceOf(OandaInvalidResponseException.class);
    }
}

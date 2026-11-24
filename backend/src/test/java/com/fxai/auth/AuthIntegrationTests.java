package com.fxai.auth;

import com.fxai.PostgresIntegrationTest;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AuthIntegrationTests extends PostgresIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper json;
    @Autowired AuthService auth;
    private static final String PASSWORD = "correct-horse-battery-staple";
    private record Session(String access, Cookie refresh, UUID user) {}

    private org.springframework.test.web.servlet.request.RequestPostProcessor csrfRequest() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        Cookie cookie = result.getResponse().getCookie("XSRF-TOKEN");
        return request -> {
            var existing = request.getCookies();
            var cookies = new java.util.ArrayList<Cookie>();
            if (existing != null) cookies.addAll(java.util.List.of(existing));
            cookies.add(cookie);
            request.setCookies(cookies.toArray(Cookie[]::new));
            request.addHeader(body.get("headerName").asString(), body.get("token").asString());
            return request;
        };
    }
    private Session register() throws Exception {
        return register(UUID.randomUUID() + "@example.com");
    }
    private Session register(String email) throws Exception {
        var result = mvc.perform(post("/api/v1/auth/register").with(csrfRequest()).contentType("application/json")
                .content(json.writeValueAsString(new AuthController.RegisterRequest(email, PASSWORD))))
                .andExpect(status().isCreated()).andExpect(header().string("Cache-Control", "no-store")).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        return new Session(body.get("accessToken").asString(), result.getResponse().getCookie("fxai_refresh"), UUID.fromString(body.get("userId").asString()));
    }
    @Test void registersHashesAndLogsInWithNormalizedEmail() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        Session session = register(email.toUpperCase());
        assertThat(session.refresh().isHttpOnly()).isTrue();
        assertThat(session.refresh().getPath()).isEqualTo("/api/v1/auth");
        assertThat(jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=?", String.class, session.user())).doesNotContain(PASSWORD);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_session WHERE token_hash=?", Integer.class, session.refresh().getValue())).isZero();
        mvc.perform(post("/api/v1/auth/login").with(csrfRequest()).contentType("application/json")
                .content(json.writeValueAsString(new AuthController.LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
    @Test void refreshRotatesAndReplayRevokesFamily() throws Exception {
        Session session = register();
        var rotated = mvc.perform(post("/api/v1/auth/refresh").with(csrfRequest()).cookie(session.refresh()))
                .andExpect(status().isOk()).andReturn().getResponse().getCookie("fxai_refresh");
        assertThat(rotated.getValue()).isNotEqualTo(session.refresh().getValue());
        mvc.perform(post("/api/v1/auth/refresh").with(csrfRequest()).cookie(session.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/refresh").with(csrfRequest()).cookie(rotated)).andExpect(status().isUnauthorized());
    }
    @Test void logoutRevokesRefreshAndClearsCookie() throws Exception {
        Session session = register();
        var response = mvc.perform(post("/api/v1/auth/logout").with(csrfRequest()).cookie(session.refresh()))
                .andExpect(status().isNoContent()).andReturn().getResponse();
        assertThat(response.getCookie("fxai_refresh").getMaxAge()).isZero();
        mvc.perform(post("/api/v1/auth/refresh").with(csrfRequest()).cookie(session.refresh())).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").with(csrfRequest())).andExpect(status().isNoContent());
    }
    @Test void expiredRefreshIsRejected() throws Exception {
        Session session = register();
        jdbc.update("UPDATE refresh_session SET expires_at=now()-interval '1 second' WHERE user_id=?", session.user());
        mvc.perform(post("/api/v1/auth/refresh").with(csrfRequest()).cookie(session.refresh())).andExpect(status().isUnauthorized());
    }
    @Test void authenticationErrorsAreGeneric() throws Exception {
        String email = UUID.randomUUID() + "@example.com";
        register(email);
        String existing = mvc.perform(post("/api/v1/auth/login").with(csrfRequest()).contentType("application/json")
                .content(json.writeValueAsString(new AuthController.LoginRequest(email, "wrong-password"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        String missing = mvc.perform(post("/api/v1/auth/login").with(csrfRequest()).contentType("application/json")
                .content(json.writeValueAsString(new AuthController.LoginRequest("missing-"+email, "wrong-password"))))
                .andExpect(status().isUnauthorized()).andReturn().getResponse().getContentAsString();
        assertThat(existing).isEqualTo(missing).doesNotContain(email);
        mvc.perform(post("/api/v1/auth/register").with(csrfRequest()).contentType("application/json")
                .content(json.writeValueAsString(new AuthController.RegisterRequest(email, PASSWORD))))
                .andExpect(status().isUnauthorized());
    }
    @Test void watchlistsSettingsAndAlertsAreIsolated() throws Exception {
        Session alice = register(); Session bob = register();
        mvc.perform(put("/api/v1/watchlist").header("Authorization", "Bearer "+alice.access()).with(csrfRequest()).contentType("application/json")
                .content("{\"symbols\":[\"USDJPY\"],\"selectedSymbol\":\"USDJPY\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/watchlist").header("Authorization", "Bearer "+alice.access()))
                .andExpect(jsonPath("$.symbols.length()").value(1)).andExpect(jsonPath("$.selectedSymbol").value("USDJPY"));
        mvc.perform(get("/api/v1/watchlist").header("Authorization", "Bearer "+bob.access()))
                .andExpect(jsonPath("$.selectedSymbol").value("EURUSD"));
        mvc.perform(put("/api/v1/settings").header("Authorization", "Bearer "+alice.access()).with(csrfRequest()).contentType("application/json")
                .content("{\"theme\":\"dark\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/settings").header("Authorization", "Bearer "+bob.access())).andExpect(jsonPath("$.theme").value("system"));
        var alert = mvc.perform(post("/api/v1/alerts").header("Authorization", "Bearer "+alice.access()).with(csrfRequest()).contentType("application/json")
                .content("{\"symbol\":\"EURUSD\",\"condition\":\"PRICE_ABOVE\",\"targetPrice\":1.2}"))
                .andExpect(status().isCreated()).andReturn();
        String id = json.readTree(alert.getResponse().getContentAsString()).get("id").asString();
        mvc.perform(get("/api/v1/alerts").header("Authorization", "Bearer "+bob.access())).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/v1/alerts/"+id).header("Authorization", "Bearer "+bob.access()).with(csrfRequest())).andExpect(status().isNotFound());
        mvc.perform(delete("/api/v1/alerts/"+id).header("Authorization", "Bearer "+alice.access()).with(csrfRequest())).andExpect(status().isNoContent());
    }
    @Test void concurrentRefreshCannotIssueTwoValidSuccessors() throws Exception {
        Session session = register();
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var start = new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.Callable<AuthService.Tokens> rotate = () -> {
                start.await();
                try { return auth.refresh(session.refresh().getValue()); }
                catch (AuthFailure failure) { return null; }
            };
            var first = executor.submit(rotate); var second = executor.submit(rotate);
            start.countDown();
            var results = java.util.stream.Stream.of(first.get(15, java.util.concurrent.TimeUnit.SECONDS), second.get(15, java.util.concurrent.TimeUnit.SECONDS))
                    .filter(java.util.Objects::nonNull).toList();
            assertThat(results).hasSize(1);
            assertThatThrownBy(() -> auth.refresh(results.getFirst().refreshToken())).isInstanceOf(AuthFailure.class);
        }
    }

    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    @Test void expiredAndTamperedAccessTokensAreRejected() throws Exception {
        Session session = register();
        var now = java.time.Instant.now();
        var claims = org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("fxai").subject(session.user().toString())
                .issuedAt(now.minusSeconds(900)).expiresAt(now.minusSeconds(300)).build();
        var expired = encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
                org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/v1/settings").header("Authorization", "Bearer " + expired)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/settings").header("Authorization", "Bearer " + session.access() + "tampered")).andExpect(status().isUnauthorized());
    }

    @Test void actualCsrfCookieAndHeaderAreRequired() throws Exception {
        var result = mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk()).andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        mvc.perform(post("/api/v1/auth/logout").cookie(result.getResponse().getCookie("XSRF-TOKEN"))
                .header(body.get("headerName").asString(), body.get("token").asString())).andExpect(status().isNoContent());
    }
}

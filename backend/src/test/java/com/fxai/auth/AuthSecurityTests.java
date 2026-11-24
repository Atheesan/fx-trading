package com.fxai.auth;

import com.fxai.user.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({AuthController.class, UserDataController.class})
@Import(SecurityConfiguration.class)
class AuthSecurityTests {
    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder decoder;
    @MockitoBean AuthService auth;
    @MockitoBean AuthProperties properties;
    @MockitoBean UserDataService data;

    @Test void privateDataRequiresAuthentication() throws Exception {
        for (String path : new String[]{"/watchlist", "/settings", "/alerts"}) {
            mvc.perform(get("/api/v1" + path)).andExpect(status().isUnauthorized());
        }
        verifyNoInteractions(data);
    }
    @Test void loginAndRefreshRequireCsrf() throws Exception {
        mvc.perform(post("/api/v1/auth/login").contentType("application/json")
                .content("{\"email\":\"user@example.com\",\"password\":\"valid-password\"}")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/refresh")).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout")).andExpect(status().isForbidden());
        verifyNoInteractions(auth);
    }
    @Test void rejectsInvalidRegistration() throws Exception {
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"invalid\",\"password\":\"short\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(auth);
    }
    @Test void usesPrincipalAsOwner() throws Exception {
        UUID owner = UUID.randomUUID();
        when(data.settings(owner)).thenReturn(new UserDataService.Settings("dark"));
        mvc.perform(get("/api/v1/settings").with(jwt().jwt(j -> j.subject(owner.toString()))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.theme").value("dark"));
        verify(data).settings(owner);
    }
    @Test void rejectsUnknownThemeAndSymbol() throws Exception {
        mvc.perform(put("/api/v1/settings").with(jwt()).with(csrf()).contentType("application/json")
                .content("{\"theme\":\"unknown\"}")).andExpect(status().isBadRequest());
        mvc.perform(put("/api/v1/watchlist").with(jwt()).with(csrf()).contentType("application/json")
                .content("{\"symbols\":[\"ABCDEF\"],\"selectedSymbol\":\"ABCDEF\"}")).andExpect(status().isBadRequest());
        verifyNoInteractions(data);
    }
    @Test void csrfEndpointIsPublicAndNotCacheable() throws Exception {
        mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}

package com.fxai.auth;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private static final String COOKIE = "fxai_refresh";
    private final AuthService service;
    private final AuthProperties properties;

    public AuthController(AuthService service, AuthProperties properties) {
        this.service = service;
        this.properties = properties;
    }

    public record RegisterRequest(@NotBlank @Email @Size(max=254) String email,
                                  @NotBlank @Size(min=12,max=128) String password) {}
    public record LoginRequest(@NotBlank @Email @Size(max=254) String email,
                               @NotBlank @Size(max=128) String password) {}
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, java.util.UUID userId) {}
    public record CsrfResponse(String token, String headerName) {}

    @GetMapping("/csrf")
    public CsrfResponse csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return new CsrfResponse(token.getToken(), token.getHeaderName());
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public TokenResponse register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return respond(service.register(request.email(), request.password()), response);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respond(service.login(request.email(), request.password()), response);
    }

    @PostMapping("/refresh")
    public TokenResponse refresh(@CookieValue(name=COOKIE, required=false) String token, HttpServletResponse response) {
        return respond(service.refresh(token), response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@CookieValue(name=COOKIE, required=false) String token, HttpServletResponse response) {
        service.logout(token);
        cookie(response, "", 0);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
    }

    private TokenResponse respond(AuthService.Tokens tokens, HttpServletResponse response) {
        cookie(response, tokens.refreshToken(), AuthService.REFRESH_SECONDS);
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return new TokenResponse(tokens.accessToken(), "Bearer", AuthService.ACCESS_SECONDS, tokens.userId());
    }

    private void cookie(HttpServletResponse response, String token, long maxAge) {
        response.addHeader(HttpHeaders.SET_COOKIE, ResponseCookie.from(COOKIE, token).httpOnly(true)
                .secure(properties.secureCookie()).sameSite("Strict").path("/api/v1/auth").maxAge(maxAge).build().toString());
    }
}

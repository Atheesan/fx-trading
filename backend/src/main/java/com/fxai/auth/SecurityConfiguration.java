package com.fxai.auth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration(proxyBeanMethods = false)
public class SecurityConfiguration {
	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder decoder,
            @org.springframework.beans.factory.annotation.Value("${fxai.auth.secure-cookie:true}") boolean secureCookie) throws Exception {
		var csrfCookies = new CookieCsrfTokenRepository();
        csrfCookies.setCookieCustomizer(cookie -> cookie.secure(secureCookie).sameSite("Strict"));
		return http.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.csrf(csrf -> csrf.csrfTokenRepository(csrfCookies)
						.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/api/v1/auth/**", "/api/v1/health", "/actuator/health", "/error").permitAll()
						.requestMatchers(HttpMethod.GET, "/api/v1/market/**", "/ws/market").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(resource -> resource.jwt(jwt -> jwt.decoder(decoder))
						.authenticationEntryPoint((request, response, exception) -> failure(response, 401)))
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint((request, response, exception) -> failure(response, 401))
						.accessDeniedHandler((request, response, exception) -> failure(response, 403)))
				.build();
	}

	private static void failure(jakarta.servlet.http.HttpServletResponse response, int status) throws java.io.IOException {
		response.setStatus(status);
		response.setContentType("application/json");
		response.getWriter().write("{\"code\":\"ACCESS_DENIED\",\"message\":\"Request denied\"}");
	}
}

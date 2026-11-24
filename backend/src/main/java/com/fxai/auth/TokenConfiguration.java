package com.fxai.auth;

import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuthProperties.class)
public class TokenConfiguration {
	@Bean
	SecretKeySpec signingKey(AuthProperties properties) {
		byte[] bytes = Base64.getDecoder().decode(properties.signingKey());
		if (bytes.length < 32) throw new IllegalArgumentException("FXAI_AUTH_SIGNING_KEY must contain at least 32 random bytes, Base64 encoded");
		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	@Bean
	JwtEncoder jwtEncoder(SecretKeySpec key) {
		return new NimbusJwtEncoder(new ImmutableSecret<>(key));
	}

	@Bean
	JwtDecoder jwtDecoder(SecretKeySpec key) {
		var decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer("fxai"));
		return decoder;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
	}
}

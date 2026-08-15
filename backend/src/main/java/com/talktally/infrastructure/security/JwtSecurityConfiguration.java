package com.talktally.infrastructure.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtSecurityProperties.class)
public class JwtSecurityConfiguration {

	@Bean
	JwtEncoder jwtEncoder(JwtSecurityProperties properties) {
		return NimbusJwtEncoder.withSecretKey(properties.secretKey())
				.algorithm(MacAlgorithm.HS256)
				.build();
	}

	@Bean
	JwtDecoder jwtDecoder(JwtSecurityProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder
				.withSecretKey(properties.secretKey())
				.macAlgorithm(MacAlgorithm.HS256)
				.build();
		OAuth2TokenValidator<Jwt> issuerAndTimestampValidator =
				JwtValidators.createDefaultWithIssuer(JwtSecurityProperties.ISSUER);
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(issuerAndTimestampValidator));
		return decoder;
	}
}

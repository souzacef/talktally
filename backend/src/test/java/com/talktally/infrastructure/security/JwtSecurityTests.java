package com.talktally.infrastructure.security;

import com.talktally.application.auth.port.IssuedAccessToken;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtSecurityTests {

	private static final String TEST_SECRET = Base64.getEncoder().encodeToString(
			"test-only-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
	private static final Instant NOW = Instant.parse("2026-08-15T00:00:00Z");

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void validTokenContainsStableSubjectIssuerAndExpiration() {
		JwtSecurityProperties properties = new JwtSecurityProperties(TEST_SECRET, 3600);
		JwtSecurityConfiguration configuration = new JwtSecurityConfiguration();
		JwtEncoder encoder = configuration.jwtEncoder(properties);
		JwtDecoder decoder = configuration.jwtDecoder(properties);
		Instant tokenTime = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		JwtAccessTokenIssuer issuer = new JwtAccessTokenIssuer(
				encoder, 3600, Clock.fixed(tokenTime, ZoneOffset.UTC));
		UserId userId = UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000001"));

		IssuedAccessToken issued = issuer.issue(userId);
		Jwt decoded = decoder.decode(issued.value());

		assertAll(
				() -> assertEquals(userId.value().toString(), decoded.getSubject()),
				() -> assertEquals(JwtSecurityProperties.ISSUER, decoded.getClaimAsString("iss")),
				() -> assertEquals(tokenTime, decoded.getIssuedAt()),
				() -> assertEquals(tokenTime.plusSeconds(3600), decoded.getExpiresAt()),
				() -> assertFalse(issued.toString().contains(issued.value())),
				() -> assertNotNull(decoded.getHeaders().get("alg")));
	}

	@Test
	void missingInvalidAndShortSecretsAreRejectedClearly() {
		String shortSecret = Base64.getEncoder().encodeToString(
				"too-short".getBytes(StandardCharsets.UTF_8));

		assertAll(
				() -> assertThrows(
						IllegalStateException.class,
						() -> new JwtSecurityProperties("", 3600)),
				() -> assertThrows(
						IllegalStateException.class,
						() -> new JwtSecurityProperties("not-valid-base64!", 3600)),
				() -> assertThrows(
						IllegalStateException.class,
						() -> new JwtSecurityProperties(shortSecret, 3600)));
	}

	@Test
	void nonPositiveTokenLifetimeIsRejected() {
		assertThrows(
				IllegalStateException.class,
				() -> new JwtSecurityProperties(TEST_SECRET, 0));
	}

	@Test
	void authenticatedUserProviderReturnsUserIdFromJwtSubject() {
		UserId expected = UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000001"));
		Jwt jwt = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.subject(expected.value().toString())
				.issuedAt(NOW)
				.expiresAt(NOW.plusSeconds(3600))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(jwt, List.of()));

		assertEquals(expected, new SecurityContextAuthenticatedUserProvider().currentUserId());
	}

	@Test
	void authenticatedUserProviderFailsSafelyWithoutValidJwtIdentity() {
		SecurityContextAuthenticatedUserProvider provider =
				new SecurityContextAuthenticatedUserProvider();
		assertThrows(AuthenticationCredentialsNotFoundException.class, provider::currentUserId);

		Jwt invalidSubject = Jwt.withTokenValue("test-token")
				.header("alg", "HS256")
				.subject("not-a-uuid")
				.issuedAt(NOW)
				.expiresAt(NOW.plusSeconds(3600))
				.build();
		SecurityContextHolder.getContext().setAuthentication(
				new JwtAuthenticationToken(invalidSubject, List.of()));

		assertThrows(BadCredentialsException.class, provider::currentUserId);
	}
}

package com.talktally.infrastructure.web.auth;

import com.talktally.application.auth.RegisterUserUseCase;
import com.talktally.application.auth.input.RegisterUserInput;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.domain.UserId;
import com.talktally.infrastructure.security.AuthenticatedUserProvider;
import com.talktally.infrastructure.security.JwtSecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD;
import static org.springframework.http.HttpHeaders.ORIGIN;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
		"talktally.web.cors.allowed-origins=https://frontend.example")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(AuthHttpSecurityIntegrationTests.ProtectedTestController.class)
class AuthHttpSecurityIntegrationTests {

	private static final String PASSWORD = "securepass123";
	private static final UserId USER_ID = UserId.from(
			UUID.fromString("10000000-0000-0000-0000-000000000001"));

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RegisterUserUseCase registerUserUseCase;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private JwtEncoder jwtEncoder;

	@Test
	void registrationEndpointIsPublicAndReturnsSafeCreatedAccount() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/auth/registrations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "  PERSON@Example.COM  ",
								  "password": "securepass123",
								  "displayName": "  Person Name  "
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$.userId").isNotEmpty())
				.andExpect(jsonPath("$.email").value("person@example.com"))
				.andExpect(jsonPath("$.displayName").value("Person Name"))
				.andExpect(jsonPath("$.defaultCurrency").value("BRL"))
				.andExpect(jsonPath("$.password").doesNotExist())
				.andExpect(jsonPath("$.passwordHash").doesNotExist())
				.andReturn();

		String body = result.getResponse().getContentAsString().toLowerCase();
		assertFalse(body.contains(PASSWORD));
		assertFalse(body.contains("password"));
		assertFalse(body.contains("hash"));
	}

	@Test
	void invalidRegistrationReturnsConsistentBadRequest() throws Exception {
		mockMvc.perform(post("/api/v1/auth/registrations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "invalid",
								  "password": "short1",
								  "displayName": "Person"
								}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REGISTRATION"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void duplicateNormalizedEmailReturnsConflict() throws Exception {
		registerAccount("person@example.com");

		mockMvc.perform(post("/api/v1/auth/registrations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": " PERSON@EXAMPLE.COM ",
								  "password": "different456",
								  "displayName": "Another"
								}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
	}

	@Test
	void loginEndpointIsPublicAndReturnsBearerTokenWithoutPasswordHash() throws Exception {
		registerAccount("person@example.com");

		MvcResult result = mockMvc.perform(post("/api/v1/auth/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": " PERSON@EXAMPLE.COM ",
								  "password": "securepass123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.expiresIn").value(3600))
				.andExpect(jsonPath("$.expiresAt").isNotEmpty())
				.andExpect(jsonPath("$.user.email").value("person@example.com"))
				.andExpect(jsonPath("$.user.password").doesNotExist())
				.andExpect(jsonPath("$.user.passwordHash").doesNotExist())
				.andReturn();

		String body = result.getResponse().getContentAsString();
		assertFalse(body.contains(PASSWORD));
		assertFalse(body.contains("passwordHash"));
	}

	@Test
	void wrongPasswordAndNonexistentAccountReturnIdenticalUnauthorizedErrors() throws Exception {
		registerAccount("person@example.com");

		MvcResult wrongPassword = login("person@example.com", "incorrect123");
		MvcResult nonexistent = login("missing@example.com", PASSWORD);

		assertEquals(401, wrongPassword.getResponse().getStatus());
		assertEquals(401, nonexistent.getResponse().getStatus());
		assertEquals(
				wrongPassword.getResponse().getContentAsString(),
				nonexistent.getResponse().getContentAsString());
	}

	@Test
	void protectedEndpointAcceptsValidBearerAndUsesServerControlledUserId() throws Exception {
		String token = accessTokenIssuer.issue(USER_ID).value();

		mockMvc.perform(get("/api/v1/test/protected")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(USER_ID.value().toString()));
	}

	@Test
	void protectedEndpointRejectsMissingAndMalformedTokens() throws Exception {
		mockMvc.perform(get("/api/v1/test/protected"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

		mockMvc.perform(get("/api/v1/test/protected")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void protectedEndpointRejectsWrongSignature() throws Exception {
		byte[] wrongKey = "different-test-only-secret-with-32-bytes"
				.getBytes(StandardCharsets.UTF_8);
		JwtEncoder wrongEncoder = NimbusJwtEncoder
				.withSecretKey(new SecretKeySpec(wrongKey, "HmacSHA256"))
				.algorithm(MacAlgorithm.HS256)
				.build();
		String token = encode(
				wrongEncoder,
				USER_ID,
				JwtSecurityProperties.ISSUER,
				Instant.now().minusSeconds(1),
				Instant.now().plusSeconds(3600));

		mockMvc.perform(get("/api/v1/test/protected")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointRejectsExpiredToken() throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String token = encode(
				jwtEncoder,
				USER_ID,
				JwtSecurityProperties.ISSUER,
				now.minusSeconds(600),
				now.minusSeconds(120));

		mockMvc.perform(get("/api/v1/test/protected")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointRejectsWrongIssuer() throws Exception {
		Instant now = Instant.now().truncatedTo(ChronoUnit.SECONDS);
		String token = encode(
				jwtEncoder,
				USER_ID,
				"not-talktally",
				now.minusSeconds(1),
				now.plusSeconds(3600));

		mockMvc.perform(get("/api/v1/test/protected")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void accessDeniedUsesConsistentForbiddenResponse() throws Exception {
		String token = accessTokenIssuer.issue(USER_ID).value();

		mockMvc.perform(get("/api/v1/test/forbidden")
						.header("Authorization", "Bearer " + token))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void actuatorHealthIsPublic() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk());
	}

	@Test
	void actuatorHealthAllowsOnlyTheConfiguredFrontendOrigin() throws Exception {
		mockMvc.perform(options("/actuator/health")
						.header(ORIGIN, "https://frontend.example")
						.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						ACCESS_CONTROL_ALLOW_ORIGIN,
						"https://frontend.example"));

		mockMvc.perform(options("/actuator/health/readiness")
						.header(ORIGIN, "https://frontend.example")
						.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isOk())
				.andExpect(header().string(
						ACCESS_CONTROL_ALLOW_ORIGIN,
						"https://frontend.example"));

		mockMvc.perform(options("/actuator/health")
						.header(ORIGIN, "https://unrelated.example")
						.header(ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden())
				.andExpect(header().doesNotExist(ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void otherActuatorEndpointsRemainUnavailable() throws Exception {
		mockMvc.perform(get("/actuator/env"))
				.andExpect(status().is4xxClientError());
	}

	private MvcResult login(String email, String password) throws Exception {
		return mockMvc.perform(post("/api/v1/auth/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "%s"
								}
								""".formatted(email, password)))
				.andReturn();
	}

	private void registerAccount(String email) {
		registerUserUseCase.execute(new RegisterUserInput(email, PASSWORD, "Person"));
	}

	private static String encode(
			JwtEncoder encoder,
			UserId userId,
			String issuer,
			Instant issuedAt,
			Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.issuer(issuer)
				.subject(userId.value().toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
		return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}

	@RestController
	static class ProtectedTestController {

		private final AuthenticatedUserProvider authenticatedUserProvider;

		ProtectedTestController(AuthenticatedUserProvider authenticatedUserProvider) {
			this.authenticatedUserProvider = authenticatedUserProvider;
		}

		@GetMapping("/api/v1/test/protected")
		Map<String, String> protectedEndpoint() {
			return Map.of(
					"userId",
					authenticatedUserProvider.currentUserId().value().toString());
		}

		@GetMapping("/api/v1/test/forbidden")
		void forbiddenEndpoint() {
			throw new AccessDeniedException("test-only denial");
		}
	}
}

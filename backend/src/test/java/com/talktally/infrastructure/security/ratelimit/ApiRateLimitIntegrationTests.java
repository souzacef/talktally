package com.talktally.infrastructure.security.ratelimit;

import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"talktally.security.rate-limit.enabled=true",
		"talktally.web.cors.allowed-origins=https://frontend.example.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ApiRateLimitIntegrationTests {

	private static final String INVALID_REGISTRATION = """
			{"email":"invalid","password":"short","displayName":"x"}
			""";
	private static final String INVALID_LOGIN = """
			{"email":"missing@example.test","password":"wrong-password"}
			""";
	private static final String BLANK_MESSAGE = """
			{"message":" "}
			""";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	void registrationAllowsThreeThenLimitsByIndependentRenderClientIp() throws Exception {
		String firstIp = "203.0.113.11";
		for (int attempt = 0; attempt < 3; attempt++) {
			registration(firstIp).andExpect(status().isBadRequest());
		}
		assertRateLimited(registration(firstIp).andReturn());
		registration("203.0.113.12").andExpect(status().isBadRequest());

		login(firstIp, INVALID_LOGIN).andExpect(status().isUnauthorized());
	}

	@Test
	void loginAllowsTenThenReturnsGenericAccountIndependentRateLimit() throws Exception {
		String ip = "203.0.113.21";
		for (int attempt = 0; attempt < 10; attempt++) {
			login(ip, INVALID_LOGIN).andExpect(status().isUnauthorized());
		}
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				UUID.fromString("10000000-0000-0000-0000-000000000121"),
				"known@example.test",
				"unused-rate-limited-hash",
				"Known User");

		MvcResult known = login(ip, """
				{"email":"known@example.test","password":"wrong-password"}
				""").andReturn();
		MvcResult missing = login(ip, INVALID_LOGIN).andReturn();
		assertRateLimited(known);
		assertRateLimited(missing);
		assertEquals(
				known.getResponse().getContentAsString(),
				missing.getResponse().getContentAsString());

		login("203.0.113.22", INVALID_LOGIN).andExpect(status().isUnauthorized());
	}

	@Test
	void textQuotaUsesAuthenticatedUserAndIsIndependentFromVoiceAndOtherUsers()
			throws Exception {
		String userAToken = token("10000000-0000-0000-0000-000000000131");
		String userBToken = token("10000000-0000-0000-0000-000000000132");

		for (int attempt = 0; attempt < 20; attempt++) {
			text(userAToken).andExpect(status().isBadRequest());
		}
		assertRateLimited(text(userAToken).andReturn());
		text(userBToken).andExpect(status().isBadRequest());
		voice(userAToken).andExpect(status().isBadRequest());
	}

	@Test
	void voiceQuotaUsesAuthenticatedUserAndDoesNotAffectOrdinaryProtectedApis()
			throws Exception {
		String token = token("10000000-0000-0000-0000-000000000141");

		for (int attempt = 0; attempt < 6; attempt++) {
			voice(token).andExpect(status().isBadRequest());
		}
		assertRateLimited(voice(token).andReturn());

		mockMvc.perform(get("/api/v1/categories")
						.header(HttpHeaders.AUTHORIZATION, bearer(token)))
				.andExpect(status().isOk());
	}

	@Test
	void unauthenticatedAssistantRequestsRemainUnauthorizedAndConsumeNoUserQuota()
			throws Exception {
		for (int attempt = 0; attempt < 25; attempt++) {
			mockMvc.perform(post("/api/v1/assistant/messages")
							.contentType(MediaType.APPLICATION_JSON)
							.content(BLANK_MESSAGE))
					.andExpect(status().isUnauthorized());
		}
		for (int attempt = 0; attempt < 10; attempt++) {
			mockMvc.perform(multipart("/api/v1/assistant/voice")
							.file(emptyAudio()))
					.andExpect(status().isUnauthorized());
		}

		text(token("10000000-0000-0000-0000-000000000151"))
				.andExpect(status().isBadRequest());
		voice(token("10000000-0000-0000-0000-000000000152"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void corsPreflightDoesNotConsumeRegistrationQuota() throws Exception {
		String ip = "203.0.113.31";
		for (int attempt = 0; attempt < 10; attempt++) {
			mockMvc.perform(options("/api/v1/auth/registrations")
							.header(HttpHeaders.ORIGIN, "https://frontend.example.test")
							.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
							.header(ClientIpAddressResolver.FORWARDED_FOR_HEADER, ip))
					.andExpect(status().isOk());
		}
		for (int attempt = 0; attempt < 3; attempt++) {
			registration(ip).andExpect(status().isBadRequest());
		}
		assertRateLimited(registration(ip).andReturn());
	}

	private org.springframework.test.web.servlet.ResultActions registration(String ip)
			throws Exception {
		return mockMvc.perform(post("/api/v1/auth/registrations")
				.header(ClientIpAddressResolver.FORWARDED_FOR_HEADER, ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content(INVALID_REGISTRATION));
	}

	private org.springframework.test.web.servlet.ResultActions login(String ip, String content)
			throws Exception {
		return mockMvc.perform(post("/api/v1/auth/sessions")
				.header(ClientIpAddressResolver.FORWARDED_FOR_HEADER, ip)
				.contentType(MediaType.APPLICATION_JSON)
				.content(content));
	}

	private org.springframework.test.web.servlet.ResultActions text(String token) throws Exception {
		return mockMvc.perform(post("/api/v1/assistant/messages")
				.header(HttpHeaders.AUTHORIZATION, bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(BLANK_MESSAGE));
	}

	private org.springframework.test.web.servlet.ResultActions voice(String token) throws Exception {
		return mockMvc.perform(multipart("/api/v1/assistant/voice")
				.file(emptyAudio())
				.header(HttpHeaders.AUTHORIZATION, bearer(token)));
	}

	private String token(String userId) {
		return accessTokenIssuer.issue(UserId.from(UUID.fromString(userId))).value();
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private static MockMultipartFile emptyAudio() {
		return new MockMultipartFile("file", "empty.wav", "audio/wav", new byte[0]);
	}

	private static void assertRateLimited(MvcResult result) throws Exception {
		assertEquals(429, result.getResponse().getStatus());
		assertEquals("application/json", result.getResponse().getContentType());
		assertEquals(
				"RATE_LIMITED",
				com.jayway.jsonpath.JsonPath.read(
						result.getResponse().getContentAsString(), "$.code"));
		assertEquals(
				"too many requests",
				com.jayway.jsonpath.JsonPath.read(
						result.getResponse().getContentAsString(), "$.message"));
		org.hamcrest.MatcherAssert.assertThat(
				result.getResponse().getHeader(HttpHeaders.RETRY_AFTER),
				matchesPattern("[1-9][0-9]*"));
	}
}

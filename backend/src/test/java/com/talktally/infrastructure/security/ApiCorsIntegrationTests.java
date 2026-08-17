package com.talktally.infrastructure.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties =
		"talktally.web.cors.allowed-origins=https://frontend.example.test,http://localhost:5173")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiCorsIntegrationTests {

	private static final String ALLOWED_ORIGIN = "https://frontend.example.test";

	@Autowired
	private MockMvc mockMvc;

	@Test
	void configuredExactOriginReceivesRequiredMethodsAndHeadersWithoutCredentials()
			throws Exception {
		MvcResult result = mockMvc.perform(options("/api/v1/transactions")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(
								HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
								"Authorization, Content-Type"))
				.andExpect(status().isOk())
				.andReturn();

		assertEquals(
				ALLOWED_ORIGIN,
				result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
		assertEquals(
				Set.of("GET", "POST", "PUT", "DELETE", "OPTIONS"),
				commaSeparatedHeader(
						result, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, false));
		assertEquals(
				Set.of("authorization", "content-type"),
				commaSeparatedHeader(
						result, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, true));
		assertNull(result.getResponse().getHeader(
				HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS));
	}

	@Test
	void unknownOriginIsRejectedWithoutCorsPermission() throws Exception {
		MvcResult result = mockMvc.perform(options("/api/v1/transactions")
						.header(HttpHeaders.ORIGIN, "https://unknown.example.test")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
				.andExpect(status().isForbidden())
				.andReturn();

		assertNull(result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void allowedOriginDoesNotBypassJwtAuthentication() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/transactions")
						.header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
				.andExpect(status().isUnauthorized())
				.andReturn();

		assertEquals(
				ALLOWED_ORIGIN,
				result.getResponse().getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
	}

	@Test
	void wildcardAndNonOriginValuesAreRejectedByConfigurationPolicy() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new ApiCorsProperties(List.of("*")));
		assertThrows(
				IllegalArgumentException.class,
				() -> new ApiCorsProperties(List.of("https://example.test/path")));
		ApiCorsProperties exact = new ApiCorsProperties(
				List.of(ALLOWED_ORIGIN, ALLOWED_ORIGIN, "  "));
		assertEquals(List.of(ALLOWED_ORIGIN), exact.allowedOrigins());
		assertFalse(exact.allowedOrigins().contains("*"));
	}

	private static Set<String> commaSeparatedHeader(
			MvcResult result,
			String name,
			boolean lowercase) {
		String header = result.getResponse().getHeader(name);
		assertTrue(header != null && !header.isBlank());
		return Arrays.stream(header.split(","))
				.map(String::strip)
				.map(value -> lowercase ? value.toLowerCase(Locale.ROOT) : value)
				.collect(Collectors.toUnmodifiableSet());
	}
}

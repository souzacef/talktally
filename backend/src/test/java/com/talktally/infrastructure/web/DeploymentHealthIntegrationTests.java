package com.talktally.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DeploymentHealthIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private WebEndpointsSupplier webEndpointsSupplier;

	@Test
	void healthLivenessAndReadinessArePublicAndHealthy() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
		mockMvc.perform(get("/actuator/health/liveness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/actuator/health/readiness"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
	}

	@Test
	void onlyHealthIsExposedAsAnActuatorWebEndpoint() {
		Set<String> exposedIds = webEndpointsSupplier.getEndpoints().stream()
				.map(endpoint -> endpoint.getEndpointId().toString())
				.collect(Collectors.toUnmodifiableSet());

		assertEquals(Set.of("health"), exposedIds);
	}

	@Test
	void protectedApiRemainsProtectedAlongsidePublicHealth() throws Exception {
		mockMvc.perform(get("/api/v1/transactions"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void emptyCorsConfigurationIsClosedByDefault() throws Exception {
		mockMvc.perform(options("/api/v1/transactions")
						.header(HttpHeaders.ORIGIN, "https://frontend.example.test")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}
}

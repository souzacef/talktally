package com.talktally.infrastructure.web.dashboard;

import com.jayway.jsonpath.JsonPath;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardApiIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000051");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000052");
	private static final UserId USER_A = UserId.from(USER_A_VALUE);
	private static final UserId USER_B = UserId.from(USER_B_VALUE);
	private static final UUID SALARY =
			UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GROCERIES =
			UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID SHOPPING =
			UUID.fromString("00000000-0000-0000-0000-000000000011");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	private String tokenA;
	private String tokenB;

	@BeforeEach
	void setUpUsers() {
		insertUser(USER_A_VALUE, "dashboard-a@example.com");
		insertUser(USER_B_VALUE, "dashboard-b@example.com");
		tokenA = accessTokenIssuer.issue(USER_A).value();
		tokenB = accessTokenIssuer.issue(USER_B).value();
	}

	@Test
	void allDashboardRoutesRequireJwt() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/dashboard/category-breakdown")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31")
						.param("kind", "EXPENSE"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/dashboard/monthly-cash-flow")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void canonicalSalaryPizzaAndJonPaymentProduceExactSummaryAndSnapshot() throws Exception {
		createTransaction(tokenA, "INCOME", "Salary", "1000.00", SALARY, 1);
		createTransaction(tokenB, "INCOME", "Other salary", "9000.00", SALARY, 1);
		UUID personId = createPerson(tokenA, "Jon Doe");
		UUID claimId = createReimbursement(tokenA, personId, "174.00", 1);
		recordPayment(tokenA, claimId, "50.00");

		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.currency").value("BRL"))
				.andExpect(jsonPath("$.period.earnedIncome").value(1000.0))
				.andExpect(jsonPath("$.period.expenses").value(174.0))
				.andExpect(jsonPath("$.period.reimbursementsReceived").value(50.0))
				.andExpect(jsonPath("$.period.netCashFlow").value(876.0))
				.andExpect(jsonPath("$.period.occurrenceCount").value(3))
				.andExpect(jsonPath("$.period.transactionCount").value(3))
				.andExpect(jsonPath("$.owedToMe.outstanding").value(124.0))
				.andExpect(jsonPath("$.owedToMe.openClaims").value(1));

		mockMvc.perform(get("/api/v1/dashboard/category-breakdown")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31")
						.param("kind", "EXPENSE")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(174.0))
				.andExpect(jsonPath("$.categories", hasSize(1)))
				.andExpect(jsonPath("$.categories[0].code").value("GROCERIES"))
				.andExpect(jsonPath("$.categories[0].percentage").value(100.0));
	}

	@Test
	void installmentReportsUseOccurrenceDatesForSummaryCategoryAndMonthlyFlow() throws Exception {
		createTransaction(tokenA, "EXPENSE", "Laptop", "100.00", SHOPPING, 3);

		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-09-01")
						.param("to", "2026-09-30")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period.expenses").value(33.33))
				.andExpect(jsonPath("$.period.transactionCount").value(1))
				.andExpect(jsonPath("$.period.occurrenceCount").value(1));
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-08-01")
						.param("to", "2026-10-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.period.expenses").value(100.0));
		mockMvc.perform(get("/api/v1/dashboard/category-breakdown")
						.param("from", "2026-09-01")
						.param("to", "2026-09-30")
						.param("kind", "EXPENSE")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.total").value(33.33))
				.andExpect(jsonPath("$.categories[0].code").value("SHOPPING"));
		mockMvc.perform(get("/api/v1/dashboard/monthly-cash-flow")
						.param("from", "2026-08-01")
						.param("to", "2026-10-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.buckets", hasSize(3)))
				.andExpect(jsonPath("$.buckets[0].month").value(8))
				.andExpect(jsonPath("$.buckets[0].expenses").value(33.33))
				.andExpect(jsonPath("$.buckets[1].month").value(9))
				.andExpect(jsonPath("$.buckets[1].expenses").value(33.33))
				.andExpect(jsonPath("$.buckets[2].month").value(10))
				.andExpect(jsonPath("$.buckets[2].expenses").value(33.34));
	}

	@Test
	void invalidDashboardParametersReturnStableBadRequests() throws Exception {
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("to", "2026-08-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "not-a-date")
						.param("to", "2026-08-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mockMvc.perform(get("/api/v1/dashboard/summary")
						.param("from", "2026-09-01")
						.param("to", "2026-08-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REPORTING_REQUEST"));
		mockMvc.perform(get("/api/v1/dashboard/category-breakdown")
						.param("from", "2026-08-01")
						.param("to", "2026-08-31")
						.param("kind", "REIMBURSEMENT_RECEIPT")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REPORTING_REQUEST"));
		mockMvc.perform(get("/api/v1/dashboard/monthly-cash-flow")
						.param("from", "2020-01-01")
						.param("to", "2025-01-01")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest());
	}

	private void createTransaction(
			String token,
			String kind,
			String description,
			String amount,
			UUID categoryId,
			int installmentCount) throws Exception {
		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "kind": "%s",
								  "description": "%s",
								  "amount": %s,
								  "categoryId": "%s",
								  "eventDate": "2026-08-14",
								  "installmentCount": %d
								}
								""".formatted(
								kind,
								description,
								amount,
								categoryId,
								installmentCount)))
				.andExpect(status().isCreated());
	}

	private UUID createPerson(String token, String name) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/people")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"%s\"}".formatted(name)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(
				result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createReimbursement(
			String token,
			UUID personId,
			String amount,
			int installmentCount) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "description": "Pizza",
								  "amount": %s,
								  "categoryId": "%s",
								  "eventDate": "2026-08-14",
								  "installmentCount": %d,
								  "personId": "%s"
								}
								""".formatted(amount, GROCERIES, installmentCount, personId)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(
				result.getResponse().getContentAsString(), "$.claim.id"));
	}

	private void recordPayment(String token, UUID claimId, String amount) throws Exception {
		mockMvc.perform(post("/api/v1/reimbursements/{id}/payments", claimId)
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "amount": %s,
								  "receivedDate": "2026-08-20"
								}
								""".formatted(amount)))
				.andExpect(status().isCreated());
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private void insertUser(UUID id, String email) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				id,
				email,
				"test-only-password-hash",
				email);
	}
}

package com.talktally.infrastructure.web.category;

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

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CategoryApiIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000041");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000042");
	private static final UserId USER_A = UserId.from(USER_A_VALUE);
	private static final UUID REIMBURSEMENT =
			UUID.fromString("00000000-0000-0000-0000-000000000014");
	private static final List<String> BUILT_IN_CODES = List.of(
			"SALARY",
			"FREELANCE",
			"FOOD_DINING",
			"GROCERIES",
			"HOUSING",
			"UTILITIES",
			"TRANSPORT",
			"HEALTH",
			"EDUCATION",
			"ENTERTAINMENT",
			"SHOPPING",
			"TRAVEL",
			"TAXES_FEES",
			"REIMBURSEMENT",
			"OTHER");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	private String tokenA;

	@BeforeEach
	void setUpUsers() {
		insertUser(USER_A_VALUE, "category-a@example.com", "Category A");
		insertUser(USER_B_VALUE, "category-b@example.com", "Category B");
		tokenA = accessTokenIssuer.issue(USER_A).value();
	}

	@Test
	void authenticatedUserListsCompleteBuiltInContractInStableOrder() throws Exception {
		MvcResult result = mockMvc.perform(get("/api/v1/categories")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(jsonPath("$", hasSize(15)))
				.andExpect(jsonPath("$[2].id")
						.value("00000000-0000-0000-0000-000000000003"))
				.andExpect(jsonPath("$[2].code").value("FOOD_DINING"))
				.andExpect(jsonPath("$[2].displayName").value("Food and dining"))
				.andExpect(jsonPath("$[2].allowedKind").value("EXPENSE"))
				.andExpect(jsonPath("$[2].builtIn").value(true))
				.andExpect(jsonPath("$[2].ownerUserId").doesNotExist())
				.andExpect(jsonPath("$[13].code").value("REIMBURSEMENT"))
				.andExpect(jsonPath("$[13].allowedKind").value("REIMBURSEMENT_RECEIPT"))
				.andExpect(jsonPath("$[14].code").value("OTHER"))
				.andExpect(jsonPath("$[14].allowedKind").value("ANY"))
				.andReturn();

		List<String> codes = JsonPath.read(result.getResponse().getContentAsString(), "$[*].code");
		assertEquals(BUILT_IN_CODES, codes);
		assertFalse(result.getResponse().getContentAsString().contains("ownerUserId"));
	}

	@Test
	void userSeesOnlyTheirCustomCategoriesAfterBuiltIns() throws Exception {
		insertCustomCategory(
				UUID.fromString("20000000-0000-0000-0000-000000000003"),
				USER_A_VALUE,
				"A_CUSTOM_LATER",
				"My later custom expense");
		insertCustomCategory(
				UUID.fromString("20000000-0000-0000-0000-000000000001"),
				USER_A_VALUE,
				"A_CUSTOM_EARLIER",
				"My earlier custom expense");
		insertCustomCategory(
				UUID.fromString("20000000-0000-0000-0000-000000000002"),
				USER_B_VALUE,
				"B_CUSTOM",
				"Another user's category");

		mockMvc.perform(get("/api/v1/categories")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(17)))
				.andExpect(jsonPath("$[15].id")
						.value("20000000-0000-0000-0000-000000000001"))
				.andExpect(jsonPath("$[15].code").value("A_CUSTOM_EARLIER"))
				.andExpect(jsonPath("$[15].displayName").value("My earlier custom expense"))
				.andExpect(jsonPath("$[15].allowedKind").value("EXPENSE"))
				.andExpect(jsonPath("$[15].builtIn").value(false))
				.andExpect(jsonPath("$[16].id")
						.value("20000000-0000-0000-0000-000000000003"))
				.andExpect(jsonPath("$[16].code").value("A_CUSTOM_LATER"))
				.andExpect(jsonPath("$[?(@.code == 'B_CUSTOM')]", hasSize(0)));
	}

	@Test
	void endpointRequiresAuthentication() throws Exception {
		mockMvc.perform(get("/api/v1/categories"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void catalogDoesNotEnableManualReimbursementReceiptCreation() throws Exception {
		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "kind": "REIMBURSEMENT_RECEIPT",
								  "description": "Manual reimbursement",
								  "amount": 10.00,
								  "categoryId": "%s",
								  "eventDate": "2026-08-19",
								  "installmentCount": 1
								}
								""".formatted(REIMBURSEMENT)))
				.andExpect(status().isBadRequest());

		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM financial_transaction WHERE user_id = ?",
				Integer.class,
				USER_A_VALUE));
	}

	private void insertUser(UUID id, String email, String displayName) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				id,
				email,
				"test-only-password-hash",
				displayName);
	}

	private void insertCustomCategory(
			UUID id,
			UUID ownerId,
			String code,
			String displayName) {
		jdbcTemplate.update("""
				INSERT INTO category
				    (id, owner_user_id, code, display_name, allowed_kind, built_in)
				VALUES (?, ?, ?, ?, 'EXPENSE', FALSE)
				""",
				id,
				ownerId,
				code,
				displayName);
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}
}

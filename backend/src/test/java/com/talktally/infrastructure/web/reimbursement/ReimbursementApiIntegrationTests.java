package com.talktally.infrastructure.web.reimbursement;

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
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ReimbursementApiIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000031");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000032");
	private static final UserId USER_A = UserId.from(USER_A_VALUE);
	private static final UserId USER_B = UserId.from(USER_B_VALUE);
	private static final UUID GROCERIES =
			UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID SALARY =
			UUID.fromString("00000000-0000-0000-0000-000000000001");

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
		insertUser(USER_A_VALUE, "reimbursement-api-a@example.com");
		insertUser(USER_B_VALUE, "reimbursement-api-b@example.com");
		tokenA = accessTokenIssuer.issue(USER_A).value();
		tokenB = accessTokenIssuer.issue(USER_B).value();
	}

	@Test
	void canonicalJonDoePizzaFlowWorksEndToEndAndProtectsLinkedTransactions() throws Exception {
		String token = registerAndLogin("jon-flow@example.com");
		UUID personId = createPerson(token, "Jon Doe");

		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "kind": "INCOME",
								  "description": "Salary",
								  "amount": 1000.00,
								  "categoryId": "%s",
								  "eventDate": "2026-08-14",
								  "installmentCount": 1
								}
								""".formatted(SALARY)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.kind").value("INCOME"))
				.andExpect(jsonPath("$.amount").value(1000.0));

		MvcResult created = mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reimbursementJson(
								"Pizza", "174.00", GROCERIES, personId, null, 3)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/reimbursements/")))
				.andExpect(jsonPath("$.expense.kind").value("EXPENSE"))
				.andExpect(jsonPath("$.expense.amount").value(174.0))
				.andExpect(jsonPath("$.expense.source").value("MANUAL"))
				.andExpect(jsonPath("$.expense.firstOccurrenceDate").value("2026-08-14"))
				.andExpect(jsonPath("$.expense.occurrences", hasSize(3)))
				.andExpect(jsonPath("$.expense.occurrences[0].amount").value(58.0))
				.andExpect(jsonPath("$.expense.occurrences[1].amount").value(58.0))
				.andExpect(jsonPath("$.expense.occurrences[2].amount").value(58.0))
				.andExpect(jsonPath("$.claim.originalAmount").value(174.0))
				.andExpect(jsonPath("$.claim.amountReimbursed").value(0.0))
				.andExpect(jsonPath("$.claim.remainingAmount").value(174.0))
				.andExpect(jsonPath("$.claim.status").value("PENDING"))
				.andReturn();
		String body = created.getResponse().getContentAsString();
		UUID claimId = UUID.fromString(JsonPath.read(body, "$.claim.id"));
		UUID expenseId = UUID.fromString(JsonPath.read(body, "$.expense.id"));

		MvcResult partial = recordPayment(token, claimId, "50.00", "2026-08-20")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.claim.amountReimbursed").value(50.0))
				.andExpect(jsonPath("$.claim.remainingAmount").value(124.0))
				.andExpect(jsonPath("$.claim.status").value("PARTIALLY_PAID"))
				.andReturn();
		UUID receiptId = UUID.fromString(JsonPath.read(
				partial.getResponse().getContentAsString(), "$.receiptTransactionId"));

		mockMvc.perform(get("/api/v1/transactions/{id}", receiptId)
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.kind").value("REIMBURSEMENT_RECEIPT"))
				.andExpect(jsonPath("$.kind").value(org.hamcrest.Matchers.not("INCOME")))
				.andExpect(jsonPath("$.amount").value(50.0))
				.andExpect(jsonPath("$.eventDate").value("2026-08-20"))
				.andExpect(jsonPath("$.firstOccurrenceDate").value("2026-08-20"))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value("2026-08-20"))
				.andExpect(jsonPath("$.source").value("MANUAL"));

		recordPayment(token, claimId, "124.00", "2026-08-24")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.claim.amountReimbursed").value(174.0))
				.andExpect(jsonPath("$.claim.remainingAmount").value(0.0))
				.andExpect(jsonPath("$.claim.status").value("PAID"));

		recordPayment(token, claimId, "0.01", "2026-08-25")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REIMBURSEMENT_REQUEST"));

		mockMvc.perform(get("/api/v1/people/{id}/reimbursements/summary", personId)
						.header("Authorization", bearer(token)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.totalOriginal").value(174.0))
				.andExpect(jsonPath("$.totalReimbursed").value(174.0))
				.andExpect(jsonPath("$.totalOutstanding").value(0.0))
				.andExpect(jsonPath("$.openClaimCount").value(0));

		assertProtected(token, expenseId);
		assertProtected(token, receiptId);
	}

	@Test
	void reimbursableExpensePropagatesAnExplicitFirstOccurrenceDate() throws Exception {
		UUID personId = createPerson(tokenA, "Delayed Person");

		mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reimbursementJson(
								"Delayed dinner",
								"90.00",
								GROCERIES,
								personId,
								null,
								"2026-09-10",
								3)))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.expense.eventDate").value("2026-08-14"))
				.andExpect(jsonPath("$.expense.firstOccurrenceDate").value("2026-09-10"))
				.andExpect(jsonPath("$.expense.occurrences[0].effectiveDate").value("2026-09-10"))
				.andExpect(jsonPath("$.expense.occurrences[1].effectiveDate").value("2026-10-10"))
				.andExpect(jsonPath("$.expense.occurrences[2].effectiveDate").value("2026-11-10"));
	}

	@Test
	void peopleAreAuthenticatedNormalizedUniqueAndOwnerScoped() throws Exception {
		mockMvc.perform(post("/api/v1/people")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"Jon Doe\"}"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/people"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/people")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized());

		createPerson(tokenA, "  Jon   Doe  ");
		createPerson(tokenB, "Jon Doe");

		mockMvc.perform(post("/api/v1/people")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"JON DOE\"}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("PERSON_ALREADY_EXISTS"));
		mockMvc.perform(get("/api/v1/people")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1)))
				.andExpect(jsonPath("$[0].displayName").value("Jon   Doe"));
		mockMvc.perform(post("/api/v1/people")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void reimbursementQueriesAreOwnerScopedAndFilterDerivedStatus() throws Exception {
		UUID jon = createPerson(tokenA, "Jon Doe");
		UUID maria = createPerson(tokenA, "Maria");
		UUID other = createPerson(tokenB, "Other");
		UUID pendingClaim = createReimbursement(
				tokenA, "Jon lunch", "100.00", GROCERIES, jon, "60.00", 1);
		UUID paidClaim = createReimbursement(
				tokenA, "Maria taxi", "30.00", GROCERIES, maria, null, 1);
		UUID otherClaim = createReimbursement(
				tokenB, "Other", "99.00", GROCERIES, other, null, 1);
		recordPayment(tokenA, paidClaim, "30.00", "2026-08-20")
				.andExpect(status().isCreated());

		mockMvc.perform(get("/api/v1/reimbursements/{id}", otherClaim)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REIMBURSEMENT_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/reimbursements")
						.param("personId", jon.toString())
						.param("status", "PENDING")
						.param("page", "0")
						.param("size", "1")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(pendingClaim.toString()))
				.andExpect(jsonPath("$.items[0].originalAmount").value(60.0))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(1))
				.andExpect(jsonPath("$.totalElements").value(1))
				.andExpect(jsonPath("$.totalPages").value(1));
		mockMvc.perform(get("/api/v1/reimbursements")
						.param("status", "PAID")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].id").value(paidClaim.toString()));
	}

	@Test
	void reimbursementValidationRejectsInvalidAmountsOwnershipCategoryAndPaging() throws Exception {
		UUID personA = createPerson(tokenA, "Jon Doe");
		UUID personB = createPerson(tokenB, "Other");

		assertCreateBadRequest(reimbursementJson(
				"Too much owed", "100.00", GROCERIES, personA, "100.01", 1));
		assertCreateBadRequest(reimbursementJson(
				"Wrong category", "100.00", SALARY, personA, null, 1));
		assertCreateBadRequest(reimbursementJson(
				"Too many installments", "200.00", GROCERIES, personA, null, 121));
		mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reimbursementJson(
								"Other person", "10.00", GROCERIES, personB, null, 1)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PERSON_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/reimbursements")
						.param("size", "101")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest());
		mockMvc.perform(get("/api/v1/reimbursements")
						.param("status", "UNKNOWN")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void paymentAndSummaryUseIndistinguishableNotFoundAcrossOwners() throws Exception {
		UUID personA = createPerson(tokenA, "Jon Doe");
		UUID personB = createPerson(tokenB, "Other");
		UUID claimA = createReimbursement(
				tokenA, "Lunch", "20.00", GROCERIES, personA, null, 1);

		recordPayment(tokenB, claimA, "10.00", "2026-08-20")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("REIMBURSEMENT_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/people/{id}/reimbursements/summary", personB)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PERSON_NOT_FOUND"));
		mockMvc.perform(post("/api/v1/reimbursements/{id}/payments", claimA)
						.contentType(MediaType.APPLICATION_JSON)
						.content(paymentJson("10.00", "2026-08-20")))
				.andExpect(status().isUnauthorized());
	}

	private void assertProtected(String token, UUID transactionId) throws Exception {
		String update = """
				{
				  "kind": "EXPENSE",
				  "description": "Attempt",
				  "amount": 10.00,
				  "categoryId": "%s",
				  "eventDate": "2026-08-14",
				  "installmentCount": 1
				}
				""".formatted(GROCERIES);
		mockMvc.perform(put("/api/v1/transactions/{id}", transactionId)
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(update))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRANSACTION_PROTECTED"));
		mockMvc.perform(delete("/api/v1/transactions/{id}", transactionId)
						.header("Authorization", bearer(token)))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("TRANSACTION_PROTECTED"));
	}

	private UUID createPerson(String token, String displayName) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/people")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"displayName\":\"%s\"}".formatted(displayName)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/people/")))
				.andReturn();
		return UUID.fromString(JsonPath.read(
				result.getResponse().getContentAsString(), "$.id"));
	}

	private UUID createReimbursement(
			String token,
			String description,
			String amount,
			UUID categoryId,
			UUID personId,
			String amountOwed,
			int installments) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(reimbursementJson(
								description,
								amount,
								categoryId,
								personId,
								amountOwed,
								installments)))
				.andExpect(status().isCreated())
				.andReturn();
		return UUID.fromString(JsonPath.read(
				result.getResponse().getContentAsString(), "$.claim.id"));
	}

	private org.springframework.test.web.servlet.ResultActions recordPayment(
			String token,
			UUID claimId,
			String amount,
			String date) throws Exception {
		return mockMvc.perform(post("/api/v1/reimbursements/{id}/payments", claimId)
				.header("Authorization", bearer(token))
				.contentType(MediaType.APPLICATION_JSON)
				.content(paymentJson(amount, date)));
	}

	private void assertCreateBadRequest(String body) throws Exception {
		mockMvc.perform(post("/api/v1/reimbursements")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	private String registerAndLogin(String email) throws Exception {
		mockMvc.perform(post("/api/v1/auth/registrations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "securepass123",
								  "displayName": "Flow User"
								}
								""".formatted(email)))
				.andExpect(status().isCreated());
		MvcResult login = mockMvc.perform(post("/api/v1/auth/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "%s",
								  "password": "securepass123"
								}
								""".formatted(email)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(login.getResponse().getContentAsString(), "$.accessToken");
	}

	private static String reimbursementJson(
			String description,
			String amount,
			UUID categoryId,
			UUID personId,
			String amountOwed,
			int installments) {
		return reimbursementJson(
				description,
				amount,
				categoryId,
				personId,
				amountOwed,
				null,
				installments);
	}

	private static String reimbursementJson(
			String description,
			String amount,
			UUID categoryId,
			UUID personId,
			String amountOwed,
			String firstOccurrenceDate,
			int installments) {
		String owedField = amountOwed == null ? "" : ",\n  \"amountOwed\": " + amountOwed;
		String firstOccurrenceDateField = firstOccurrenceDate == null
				? ""
				: ",\n  \"firstOccurrenceDate\": \"" + firstOccurrenceDate + "\"";
		return """
				{
				  "description": "%s",
				  "amount": %s,
				  "categoryId": "%s",
				  "eventDate": "2026-08-14"%s,
				  "installmentCount": %d,
				  "personId": "%s"%s,
				  "note": "Dinner"
				}
				""".formatted(
				description,
				amount,
				categoryId,
				firstOccurrenceDateField,
				installments,
				personId,
				owedField);
	}

	private static String paymentJson(String amount, String date) {
		return """
				{
				  "amount": %s,
				  "receivedDate": "%s",
				  "note": "Pix"
				}
				""".formatted(amount, date);
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

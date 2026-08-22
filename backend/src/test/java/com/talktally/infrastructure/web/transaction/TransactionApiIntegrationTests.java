package com.talktally.infrastructure.web.transaction;

import com.jayway.jsonpath.JsonPath;
import com.talktally.application.auth.port.AccessTokenIssuer;
import com.talktally.application.input.CreateTransactionInput;
import com.talktally.application.output.TransactionOutput;
import com.talktally.application.transaction.CreateTransactionUseCase;
import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
class TransactionApiIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000011");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000012");
	private static final UserId USER_A = UserId.from(USER_A_VALUE);
	private static final UserId USER_B = UserId.from(USER_B_VALUE);
	private static final UUID SALARY =
			UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GROCERIES =
			UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final UUID SHOPPING =
			UUID.fromString("00000000-0000-0000-0000-000000000011");
	private static final UUID REIMBURSEMENT =
			UUID.fromString("00000000-0000-0000-0000-000000000014");
	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private AccessTokenIssuer accessTokenIssuer;

	@Autowired
	private CreateTransactionUseCase createTransactionUseCase;

	private String tokenA;
	private String tokenB;

	@BeforeEach
	void setUpUsers() {
		insertUser(USER_A_VALUE, "transaction-a@example.com", "Transaction A");
		insertUser(USER_B_VALUE, "transaction-b@example.com", "Transaction B");
		tokenA = accessTokenIssuer.issue(USER_A).value();
		tokenB = accessTokenIssuer.issue(USER_B).value();
	}

	@Test
	void completesRegisterLoginAndTransactionFlowThroughHttp() throws Exception {
		mockMvc.perform(post("/api/v1/auth/registrations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "flow@example.com",
								  "password": "securepass123",
								  "displayName": "Flow User"
								}
								"""))
				.andExpect(status().isCreated());

		MvcResult login = mockMvc.perform(post("/api/v1/auth/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "email": "flow@example.com",
								  "password": "securepass123"
								}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessToken").isNotEmpty())
				.andReturn();
		String token = JsonPath.read(
				login.getResponse().getContentAsString(), "$.accessToken");

		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(transactionJson(
								"EXPENSE", "First purchase", "19.90", GROCERIES, EVENT_DATE, 1)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", startsWith("/api/v1/transactions/")))
				.andExpect(jsonPath("$.description").value("First purchase"))
				.andExpect(jsonPath("$.source").value("MANUAL"))
				.andExpect(jsonPath("$.managedByReimbursement").value(false));
	}

	@Test
	void allTransactionRoutesRequireAValidBearerToken() throws Exception {
		String id = UUID.randomUUID().toString();
		String body = transactionJson(
				"EXPENSE", "Protected", "10.00", GROCERIES, EVENT_DATE, 1);

		mockMvc.perform(post("/api/v1/transactions")
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
		mockMvc.perform(get("/api/v1/transactions"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/transactions/{id}", id))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(delete("/api/v1/transactions/{id}", id))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void createsIncomeExpenseAndExactOrderedInstallments() throws Exception {
		MvcResult expense = create(tokenA, transactionJson(
				"EXPENSE", "  Groceries  ", "87.45", GROCERIES, EVENT_DATE, 1));
		UUID expenseId = locationId(expense);

		MvcResult persistedExpense = mockMvc.perform(get("/api/v1/transactions/{id}", expenseId)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(expenseId.toString()))
				.andExpect(jsonPath("$.kind").value("EXPENSE"))
				.andExpect(jsonPath("$.description").value("Groceries"))
				.andExpect(jsonPath("$.amount").value(87.45))
				.andExpect(jsonPath("$.currency").value("BRL"))
				.andExpect(jsonPath("$.categoryId").value(GROCERIES.toString()))
				.andExpect(jsonPath("$.eventDate").value(EVENT_DATE.toString()))
				.andExpect(jsonPath("$.firstOccurrenceDate").value(EVENT_DATE.toString()))
				.andExpect(jsonPath("$.source").value("MANUAL"))
				.andExpect(jsonPath("$.installmentCount").value(1))
				.andExpect(jsonPath("$.managedByReimbursement").value(false))
				.andExpect(jsonPath("$.createdAt").isNotEmpty())
				.andExpect(jsonPath("$.updatedAt").isNotEmpty())
				.andExpect(jsonPath("$.ownerId").doesNotExist())
				.andExpect(jsonPath("$.userId").doesNotExist())
				.andReturn();
		OffsetDateTime databaseCreatedAt = jdbcTemplate.queryForObject(
				"SELECT created_at FROM financial_transaction WHERE id = ?",
				OffsetDateTime.class,
				expenseId);
		OffsetDateTime databaseUpdatedAt = jdbcTemplate.queryForObject(
				"SELECT updated_at FROM financial_transaction WHERE id = ?",
				OffsetDateTime.class,
				expenseId);
		assertEquals(
				databaseCreatedAt.toInstant(),
				Instant.parse(JsonPath.read(
						persistedExpense.getResponse().getContentAsString(), "$.createdAt")));
		assertEquals(
				databaseUpdatedAt.toInstant(),
				Instant.parse(JsonPath.read(
						persistedExpense.getResponse().getContentAsString(), "$.updatedAt")));

		create(tokenA, transactionJson(
				"INCOME", "Salary", "5000.00", SALARY, EVENT_DATE, 1));

		MvcResult installments = create(tokenA, transactionJson(
				"EXPENSE", "Laptop", "100.00", SHOPPING, EVENT_DATE, 3));
		UUID installmentId = locationId(installments);
		mockMvc.perform(get("/api/v1/transactions/{id}", installmentId)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.occurrences", hasSize(3)))
				.andExpect(jsonPath("$.occurrences[0].sequenceNumber").value(1))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value("2026-08-14"))
				.andExpect(jsonPath("$.occurrences[0].amount").value(33.33))
				.andExpect(jsonPath("$.occurrences[1].sequenceNumber").value(2))
				.andExpect(jsonPath("$.occurrences[1].effectiveDate").value("2026-09-14"))
				.andExpect(jsonPath("$.occurrences[1].amount").value(33.33))
				.andExpect(jsonPath("$.occurrences[2].sequenceNumber").value(3))
				.andExpect(jsonPath("$.occurrences[2].effectiveDate").value("2026-10-14"))
				.andExpect(jsonPath("$.occurrences[2].amount").value(33.34));
	}

	@Test
	void createAndUpdateAcceptExplicitFirstOccurrenceDateAndOmissionUsesEventDate() throws Exception {
		MvcResult explicitNull = create(tokenA, """
				{
				  "kind": "EXPENSE",
				  "description": "Explicit null schedule",
				  "amount": 10.00,
				  "categoryId": "%s",
				  "eventDate": "2026-08-14",
				  "firstOccurrenceDate": null,
				  "installmentCount": 1
				}
				""".formatted(GROCERIES));
		assertEquals(
				EVENT_DATE.toString(),
				JsonPath.read(
						explicitNull.getResponse().getContentAsString(),
						"$.firstOccurrenceDate"));

		LocalDate firstOccurrenceDate = LocalDate.of(2026, 9, 10);
		UUID id = locationId(create(tokenA, transactionJson(
				"EXPENSE",
				"Delayed laptop",
				"100.00",
				SHOPPING,
				EVENT_DATE,
				firstOccurrenceDate,
				3)));

		mockMvc.perform(get("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventDate").value("2026-08-14"))
				.andExpect(jsonPath("$.firstOccurrenceDate").value("2026-09-10"))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value("2026-09-10"))
				.andExpect(jsonPath("$.occurrences[1].effectiveDate").value("2026-10-10"))
				.andExpect(jsonPath("$.occurrences[2].effectiveDate").value("2026-11-10"));

		LocalDate updatedFirstOccurrenceDate = LocalDate.of(2026, 10, 5);
		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(transactionJson(
								"EXPENSE",
								"Rescheduled laptop",
								"100.00",
								SHOPPING,
								EVENT_DATE,
								updatedFirstOccurrenceDate,
								3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.firstOccurrenceDate").value("2026-10-05"))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value("2026-10-05"));

		LocalDate updatedEventDate = EVENT_DATE.plusDays(5);
		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(transactionJson(
								"EXPENSE", "Reset schedule", "100.00", SHOPPING, updatedEventDate, 3)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.eventDate").value(updatedEventDate.toString()))
				.andExpect(jsonPath("$.firstOccurrenceDate").value(updatedEventDate.toString()))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value(updatedEventDate.toString()));
	}

	@Test
	void rejectsInvalidCreateBodiesCategoriesAndManualReimbursements() throws Exception {
		assertBadRequest(transactionJson(
				"EXPENSE", "Zero", "0.00", GROCERIES, EVENT_DATE, 1));
		assertBadRequest(transactionJson(
				"EXPENSE", "   ", "10.00", GROCERIES, EVENT_DATE, 1));
		assertBadRequest("""
				{
				  "kind": "EXPENSE",
				  "description": "Missing category",
				  "amount": 10.00,
				  "eventDate": "2026-08-14",
				  "installmentCount": 1
				}
				""");
		assertBadRequest(transactionJson(
				"EXPENSE", "No installments", "10.00", GROCERIES, EVENT_DATE, 0));
		assertBadRequest(transactionJson(
				"EXPENSE", "Too many", "200.00", GROCERIES, EVENT_DATE, 121));
		assertBadRequest(transactionJson(
				"EXPENSE", "Wrong category", "10.00", SALARY, EVENT_DATE, 1));
		assertBadRequest(transactionJson(
				"EXPENSE", "Unavailable", "10.00", UUID.randomUUID(), EVENT_DATE, 1));
		assertBadRequest(transactionJson(
				"REIMBURSEMENT_RECEIPT", "Receipt", "10.00",
				REIMBURSEMENT, EVENT_DATE, 1));
		assertBadRequest("{not-json}");
	}

	@Test
	void getIsOwnerScopedAndInvalidIdentifiersAreBadRequests() throws Exception {
		UUID id = locationId(create(tokenA, transactionJson(
				"EXPENSE", "Private", "10.00", GROCERIES, EVENT_DATE, 1)));

		mockMvc.perform(get("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenB)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"))
				.andExpect(jsonPath("$.message").value("transaction not found"));
		mockMvc.perform(get("/api/v1/transactions/{id}", UUID.randomUUID())
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/transactions/not-a-uuid")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	@Test
	void listsOnlyOwnedTransactionsAndAppliesKindCategoryAndSearchFilters() throws Exception {
		create(tokenA, transactionJson(
				"EXPENSE", "Weekend Groceries", "40.00", GROCERIES, EVENT_DATE, 1));
		create(tokenA, transactionJson(
				"EXPENSE", "New shoes", "80.00", SHOPPING, EVENT_DATE.minusDays(1), 1));
		create(tokenA, transactionJson(
				"INCOME", "Monthly Salary", "5000.00", SALARY, EVENT_DATE, 1));
		create(tokenB, transactionJson(
				"EXPENSE", "Other owner's groceries", "20.00", GROCERIES, EVENT_DATE, 1));

		mockMvc.perform(get("/api/v1/transactions")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(3)))
				.andExpect(jsonPath("$.page").value(0))
				.andExpect(jsonPath("$.size").value(20))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(1));
		mockMvc.perform(get("/api/v1/transactions")
						.param("kind", "INCOME")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].kind").value("INCOME"));
		mockMvc.perform(get("/api/v1/transactions")
						.param("categoryId", GROCERIES.toString())
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].description").value("Weekend Groceries"));
		mockMvc.perform(get("/api/v1/transactions")
						.param("search", "groCER")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].description").value("Weekend Groceries"));
	}

	@Test
	void dateFiltersUseEffectiveOccurrencesWithoutDuplicates() throws Exception {
		create(tokenA, transactionJson(
				"EXPENSE", "Installments", "90.00", SHOPPING, EVENT_DATE, 3));
		create(tokenA, transactionJson(
				"EXPENSE", "Outside", "10.00", GROCERIES, EVENT_DATE.minusMonths(2), 1));

		mockMvc.perform(get("/api/v1/transactions")
						.param("from", "2026-09-01")
						.param("to", "2026-10-31")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.items[0].description").value("Installments"))
				.andExpect(jsonPath("$.items[0].occurrences", hasSize(3)))
				.andExpect(jsonPath("$.totalElements").value(1));
	}

	@Test
	void paginatesAndRejectsInvalidListParameters() throws Exception {
		create(tokenA, transactionJson(
				"EXPENSE", "First", "10.00", GROCERIES, EVENT_DATE.minusDays(2), 1));
		create(tokenA, transactionJson(
				"EXPENSE", "Second", "10.00", GROCERIES, EVENT_DATE.minusDays(1), 1));
		create(tokenA, transactionJson(
				"EXPENSE", "Third", "10.00", GROCERIES, EVENT_DATE, 1));

		mockMvc.perform(get("/api/v1/transactions")
						.param("page", "1")
						.param("size", "2")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andExpect(jsonPath("$.page").value(1))
				.andExpect(jsonPath("$.size").value(2))
				.andExpect(jsonPath("$.totalElements").value(3))
				.andExpect(jsonPath("$.totalPages").value(2));

		assertListBadRequest("page", "-1");
		assertListBadRequest("size", "0");
		assertListBadRequest("size", "101");
		mockMvc.perform(get("/api/v1/transactions")
						.param("from", "2026-08-15")
						.param("to", "2026-08-14")
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRANSACTION"));
		assertListBadRequest("kind", "UNKNOWN");
		assertListBadRequest("categoryId", "not-a-uuid");
	}

	@Test
	void updateIsOwnerScopedRegeneratesScheduleAndPreservesIdOwnerAndSource() throws Exception {
		TransactionOutput created = createTransactionUseCase.execute(
				USER_A,
				TransactionSource.ASSISTANT_TEXT,
				new CreateTransactionInput(
						TransactionKind.EXPENSE,
						"Assistant created",
						new BigDecimal("10.00"),
						CategoryId.from(GROCERIES),
						EVENT_DATE,
						null,
						1));
		UUID id = created.transactionId().value();
		String update = transactionJson(
				"EXPENSE", "Edited", "100.00", SHOPPING, EVENT_DATE.plusDays(5), 3);

		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(update))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id.toString()))
				.andExpect(jsonPath("$.description").value("Edited"))
				.andExpect(jsonPath("$.source").value("ASSISTANT_TEXT"))
				.andExpect(jsonPath("$.occurrences", hasSize(3)))
				.andExpect(jsonPath("$.occurrences[0].effectiveDate").value("2026-08-19"))
				.andExpect(jsonPath("$.occurrences[2].effectiveDate").value("2026-10-19"))
				.andExpect(jsonPath("$.occurrences[2].amount").value(33.34));

		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenB))
						.contentType(MediaType.APPLICATION_JSON)
						.content(update))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
		mockMvc.perform(get("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("ASSISTANT_TEXT"));
	}

	@Test
	void updateRejectsInvalidInputCategoryAndReimbursementKind() throws Exception {
		UUID id = locationId(create(tokenA, transactionJson(
				"EXPENSE", "Original", "10.00", GROCERIES, EVENT_DATE, 1)));

		assertUpdateBadRequest(id, transactionJson(
				"EXPENSE", "Invalid amount", "0.00", GROCERIES, EVENT_DATE, 1));
		assertUpdateBadRequest(id, transactionJson(
				"EXPENSE", "Wrong", "10.00", SALARY, EVENT_DATE, 1));
		assertUpdateBadRequest(id, transactionJson(
				"REIMBURSEMENT_RECEIPT", "Receipt", "10.00",
				REIMBURSEMENT, EVENT_DATE, 1));
	}

	@Test
	void deleteIsOwnerScopedAndReturnsNoContent() throws Exception {
		UUID id = locationId(create(tokenA, transactionJson(
				"EXPENSE", "Delete me", "10.00", GROCERIES, EVENT_DATE, 1)));

		mockMvc.perform(delete("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenB)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("TRANSACTION_NOT_FOUND"));
		mockMvc.perform(delete("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNoContent())
				.andExpect(content().string(""));
		mockMvc.perform(get("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound());
		mockMvc.perform(delete("/api/v1/transactions/{id}", UUID.randomUUID())
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isNotFound());
	}

	private MvcResult create(String token, String body) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(token))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isCreated())
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(header().string("Location", startsWith("/api/v1/transactions/")))
				.andReturn();
		String location = result.getResponse().getHeader("Location");
		assertEquals("/api/v1/transactions/" + locationId(result), location);
		return result;
	}

	private void assertBadRequest(String body) throws Exception {
		mockMvc.perform(post("/api/v1/transactions")
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	private void assertUpdateBadRequest(UUID id, String body) throws Exception {
		mockMvc.perform(put("/api/v1/transactions/{id}", id)
						.header("Authorization", bearer(tokenA))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.message").isNotEmpty());
	}

	private void assertListBadRequest(String name, String value) throws Exception {
		mockMvc.perform(get("/api/v1/transactions")
						.param(name, value)
						.header("Authorization", bearer(tokenA)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").isNotEmpty())
				.andExpect(jsonPath("$.message").isNotEmpty());
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

	private static UUID locationId(MvcResult result) {
		String location = result.getResponse().getHeader("Location");
		assertTrue(location != null && location.startsWith("/api/v1/transactions/"));
		return UUID.fromString(location.substring(location.lastIndexOf('/') + 1));
	}

	private static String bearer(String token) {
		return "Bearer " + token;
	}

	private static String transactionJson(
			String kind,
			String description,
			String amount,
			UUID categoryId,
			LocalDate eventDate,
			int installmentCount) {
		return transactionJson(
				kind,
				description,
				amount,
				categoryId,
				eventDate,
				null,
				installmentCount);
	}

	private static String transactionJson(
			String kind,
			String description,
			String amount,
			UUID categoryId,
			LocalDate eventDate,
			LocalDate firstOccurrenceDate,
			int installmentCount) {
		String firstOccurrenceDateField = firstOccurrenceDate == null
				? ""
				: ",\n  \"firstOccurrenceDate\": \"" + firstOccurrenceDate + "\"";
		return """
				{
				  "kind": "%s",
				  "description": "%s",
				  "amount": %s,
				  "categoryId": "%s",
				  "eventDate": "%s"%s,
				  "installmentCount": %d
				}
				""".formatted(
				kind,
				description,
				amount,
				categoryId,
				eventDate,
				firstOccurrenceDateField,
				installmentCount);
	}
}

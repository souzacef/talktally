package com.talktally.infrastructure.ai;

import com.talktally.application.person.ResolveOrCreatePersonUseCase;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(AssistantToolsIntegrationTests.FixedClockConfiguration.class)
class AssistantToolsIntegrationTests {

	private static final UUID USER_A_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000081");
	private static final UUID USER_B_VALUE =
			UUID.fromString("10000000-0000-0000-0000-000000000082");
	private static final UserId USER_A = UserId.from(USER_A_VALUE);
	private static final UserId USER_B = UserId.from(USER_B_VALUE);
	private static final LocalDate DATE = LocalDate.of(2026, 8, 15);

	@Autowired
	private TransactionAssistantTools transactionTools;

	@Autowired
	private ReportingAssistantTools reportingTools;

	@Autowired
	private ReimbursementAssistantTools reimbursementTools;

	@Autowired
	private ResolveOrCreatePersonUseCase resolveOrCreatePersonUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUpUsers() {
		insertUser(USER_A_VALUE, "tools-a@example.com");
		insertUser(USER_B_VALUE, "tools-b@example.com");
	}

	@Test
	void toolIdentityAndSourceComeOnlyFromValidatedToolContext() {
		for (Class<?> toolClass : new Class<?>[] {
				TransactionAssistantTools.class,
				ReportingAssistantTools.class,
				ReimbursementAssistantTools.class }) {
			Arrays.stream(toolClass.getDeclaredMethods())
					.filter(method -> method.isAnnotationPresent(Tool.class))
					.flatMap(method -> Arrays.stream(method.getParameterTypes()))
					.forEach(type -> {
						assertFalse(type.equals(UserId.class));
						assertFalse(type.equals(TransactionSource.class));
					});
		}

		ToolResult result = recordExpense("Actor-owned expense", "10.00", "GROCERIES", 1, context(USER_B));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		Map<String, Object> persisted = jdbcTemplate.queryForMap("""
				SELECT user_id, source
				FROM financial_transaction
				WHERE description = 'Actor-owned expense'
				""");
		assertEquals(USER_B_VALUE, persisted.get("user_id"));
		assertEquals("ASSISTANT_TEXT", persisted.get("source"));
	}

	@Test
	void missingOrInvalidToolContextFailsInternallyWithoutWriting() {
		assertThrows(IllegalStateException.class,
				() -> recordExpense("No actor", "10.00", "GROCERIES", 1,
						new ToolContext(Map.of())));
		ToolContext invalidSource = new ToolContext(Map.of(
				AssistantToolContext.USER_ID, USER_A_VALUE.toString(),
				AssistantToolContext.TRANSACTION_SOURCE, "MODEL_CHOSEN"));
		assertThrows(IllegalStateException.class,
				() -> recordExpense("Bad source", "10.00", "GROCERIES", 1, invalidSource));
		assertEquals(0, transactionCount());
	}

	@Test
	void recordsExpenseAndIncomeThroughCategoryCodeResolution() {
		ToolResult expense = recordExpense("Groceries", "42.00", "groceries", 1, context(USER_A));
		ToolResult income = transactionTools.recordTransaction(
				"INCOME", "Salary", new BigDecimal("1000.00"), "SALARY", DATE, 1, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, expense.status());
		assertEquals(ToolResultStatus.SUCCESS, income.status());
		assertEquals(2, transactionCount());
		assertEquals(1, jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM financial_transaction transaction
				JOIN category ON category.id = transaction.category_id
				WHERE category.code = 'GROCERIES'
				""", Integer.class));
	}

	@Test
	void missingCategoryUsesDeterministicOtherFallback() {
		ToolResult result = transactionTools.recordTransaction(
				"EXPENSE",
				"Uncertain category",
				new BigDecimal("19.90"),
				null,
				null,
				1,
				context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		assertEquals("OTHER", jdbcTemplate.queryForObject("""
				SELECT category.code
				FROM financial_transaction transaction
				JOIN category ON category.id = transaction.category_id
				WHERE transaction.description = 'Uncertain category'
				""", String.class));
		assertEquals(DATE, jdbcTemplate.queryForObject(
				"SELECT event_date FROM financial_transaction", LocalDate.class));
	}

	@Test
	void missingTransactionAmountOrDescriptionClarifiesWithoutPersistence() {
		ToolResult missingAmount = transactionTools.recordTransaction(
				"EXPENSE", "Pizza", null, "FOOD_DINING", DATE, 1, context(USER_A));
		ToolResult missingDescription = transactionTools.recordTransaction(
				"EXPENSE", " ", new BigDecimal("50.00"), "FOOD_DINING", DATE, 1, context(USER_A));

		assertEquals(ToolResultStatus.NEEDS_CLARIFICATION, missingAmount.status());
		assertEquals(ToolResultStatus.NEEDS_CLARIFICATION, missingDescription.status());
		assertEquals(0, transactionCount());
	}

	@Test
	void installmentCreationDelegatesToDeterministicTransactionUseCase() {
		ToolResult result = recordExpense("Laptop", "100.00", "SHOPPING", 3, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		assertEquals(3, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM transaction_occurrence", Integer.class));
		assertEquals(0, new BigDecimal("100.00").compareTo(jdbcTemplate.queryForObject(
				"SELECT SUM(amount) FROM transaction_occurrence", BigDecimal.class)));
	}

	@Test
	void reimbursableExpenseResolvesExistingPersonAndPreservesSource() {
		resolveOrCreatePersonUseCase.execute(USER_A, "Jon Doe");

		ToolResult result = recordReimbursable(
				"Pizza", "174.00", "FOOD_DINING", 3, "jon   doe", null, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		assertEquals(1, personCount());
		assertEquals(1, claimCount());
		assertEquals("ASSISTANT_TEXT", jdbcTemplate.queryForObject(
				"SELECT source FROM financial_transaction", String.class));
		assertEquals(3, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM transaction_occurrence", Integer.class));
	}

	@Test
	void reimbursableExpenseCreatesNewPersonAndDefaultsAmountOwed() {
		ToolResult result = recordReimbursable(
				"Tickets", "80.00", null, null, "New Person", null, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		assertEquals(1, personCount());
		assertEquals(0, new BigDecimal("80.00").compareTo(jdbcTemplate.queryForObject(
				"SELECT original_amount FROM reimbursement_claim", BigDecimal.class)));
	}

	@Test
	void missingReimbursableAmountOrPersonClarifiesWithoutAnyWrite() {
		ToolResult missingAmount = reimbursementTools.recordReimbursableExpense(
				"Pizza", null, "FOOD_DINING", DATE, 87, "Jon Doe", null, null, context(USER_A));
		ToolResult missingPerson = reimbursementTools.recordReimbursableExpense(
				"Pizza", new BigDecimal("174.00"), "FOOD_DINING", DATE, 3, null, null, null, context(USER_A));

		assertEquals(ToolResultStatus.NEEDS_CLARIFICATION, missingAmount.status());
		assertEquals(ToolResultStatus.NEEDS_CLARIFICATION, missingPerson.status());
		assertEquals(0, personCount());
		assertEquals(0, transactionCount());
		assertEquals(0, claimCount());
	}

	@Test
	void transactionSearchReturnsDeterministicBoundedResults() {
		recordExpense("Needle groceries", "42.00", "GROCERIES", 1, context(USER_A));

		ToolResult result = transactionTools.searchTransactions(
				"EXPENSE", "GROCERIES", DATE, DATE, "Needle", null, null, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		var data = (TransactionAssistantTools.TransactionSearchData) result.data();
		assertEquals(1, data.transactions().size());
		assertEquals(20, data.size());
	}

	@Test
	void financialSummaryUsesDeterministicReportingOutput() {
		recordExpense("Food", "42.00", "GROCERIES", 1, context(USER_A));

		ToolResult result = reportingTools.getFinancialSummary(DATE, DATE, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		var output = (com.talktally.application.reporting.FinancialSummaryOutput) result.data();
		assertEquals(0, new BigDecimal("42.00").compareTo(output.period().expenses()));
	}

	@Test
	void categoryBreakdownAndMonthlyCashFlowUseDeterministicOutputs() {
		recordExpense("Food", "42.00", "GROCERIES", 1, context(USER_A));

		ToolResult categories = reportingTools.getCategoryBreakdown(
				DATE, DATE, "EXPENSE", context(USER_A));
		ToolResult months = reportingTools.getMonthlyCashFlow(DATE, DATE, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, categories.status());
		assertEquals(ToolResultStatus.SUCCESS, months.status());
		var categoryOutput = (ReportingAssistantTools.CategoryBreakdownData) categories.data();
		var monthlyOutput = (com.talktally.application.reporting.MonthlyCashFlowOutput) months.data();
		assertEquals("GROCERIES", categoryOutput.categories().getFirst().code());
		assertEquals(1, monthlyOutput.buckets().size());
		assertEquals(0, new BigDecimal("42.00").compareTo(monthlyOutput.buckets().getFirst().expenses()));
	}

	@Test
	void reimbursementListAndAmountOwedUseDeterministicQueries() {
		recordReimbursable("Pizza", "174.00", "FOOD_DINING", 3, "Jon Doe", null, context(USER_A));

		ToolResult list = reimbursementTools.listReimbursements(
				"Jon Doe", "PENDING", null, null, context(USER_A));
		ToolResult owed = reimbursementTools.getAmountOwedByPerson("jon doe", context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, list.status());
		assertEquals(ToolResultStatus.SUCCESS, owed.status());
		var listData = (ReimbursementAssistantTools.ReimbursementSearchData) list.data();
		var owedData = (ReimbursementAssistantTools.AmountOwedData) owed.data();
		assertEquals(1, listData.reimbursements().size());
		assertEquals(0, new BigDecimal("174.00").compareTo(owedData.totalOutstanding()));
		assertEquals(1, owedData.openClaimCount());
	}

	@Test
	void exactlyOneOpenClaimReceivesPayment() {
		recordReimbursable("Pizza", "174.00", "FOOD_DINING", 1, "Jon Doe", null, context(USER_A));

		ToolResult result = reimbursementTools.recordReimbursementPayment(
				"Jon Doe", new BigDecimal("50.00"), DATE, null, null, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, result.status());
		assertEquals(1, paymentCount());
		assertEquals(2, transactionCount());
		assertEquals("REIMBURSEMENT_RECEIPT", jdbcTemplate.queryForObject("""
				SELECT kind FROM financial_transaction WHERE kind = 'REIMBURSEMENT_RECEIPT'
				""", String.class));
	}

	@Test
	void noOpenClaimReturnsNotFoundWithoutReceipt() {
		resolveOrCreatePersonUseCase.execute(USER_A, "Jon Doe");

		ToolResult result = reimbursementTools.recordReimbursementPayment(
				"Jon Doe", new BigDecimal("50.00"), DATE, null, null, context(USER_A));

		assertEquals(ToolResultStatus.NOT_FOUND, result.status());
		assertEquals(0, paymentCount());
		assertEquals(0, transactionCount());
	}

	@Test
	void multipleOpenClaimsRequireExplicitClarificationAndNeverAllocate() {
		recordReimbursable("Pizza", "174.00", "FOOD_DINING", 1, "Jon Doe", null, context(USER_A));
		recordReimbursable("Tickets", "80.00", "OTHER", 1, "Jon Doe", null, context(USER_A));

		ToolResult result = reimbursementTools.recordReimbursementPayment(
				"Jon Doe", new BigDecimal("50.00"), DATE, null, null, context(USER_A));

		assertEquals(ToolResultStatus.NEEDS_CLARIFICATION, result.status());
		assertTrue(result.data() instanceof java.util.List<?> candidates && candidates.size() == 2);
		assertEquals(0, paymentCount());
		assertEquals(2, transactionCount());
	}

	@Test
	void explicitClaimDisambiguatesAndOverpaymentIsRejectedSafely() {
		ToolResult created = recordReimbursable(
				"Pizza", "174.00", "FOOD_DINING", 1, "Jon Doe", null, context(USER_A));
		String claimId = ((ReimbursementAssistantTools.ReimbursableExpenseData) created.data()).claimId();
		recordReimbursable("Tickets", "80.00", "OTHER", 1, "Jon Doe", null, context(USER_A));

		ToolResult selected = reimbursementTools.recordReimbursementPayment(
				"Jon Doe", new BigDecimal("50.00"), DATE, null, claimId, context(USER_A));
		ToolResult overpayment = reimbursementTools.recordReimbursementPayment(
				"Jon Doe", new BigDecimal("125.00"), DATE, null, claimId, context(USER_A));

		assertEquals(ToolResultStatus.SUCCESS, selected.status());
		assertEquals(ToolResultStatus.REJECTED, overpayment.status());
		assertEquals(1, paymentCount());
		assertEquals(3, transactionCount());
	}

	private ToolResult recordExpense(
			String description,
			String amount,
			String category,
			Integer installments,
			ToolContext context) {
		return transactionTools.recordTransaction(
				"EXPENSE", description, new BigDecimal(amount), category, DATE, installments, context);
	}

	private ToolResult recordReimbursable(
			String description,
			String amount,
			String category,
			Integer installments,
			String person,
			BigDecimal amountOwed,
			ToolContext context) {
		return reimbursementTools.recordReimbursableExpense(
				description,
				new BigDecimal(amount),
				category,
				DATE,
				installments,
				person,
				amountOwed,
				null,
				context);
	}

	private static ToolContext context(UserId userId) {
		return new ToolContext(AssistantToolContext.create(
				userId, TransactionSource.ASSISTANT_TEXT));
	}

	private int transactionCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM financial_transaction", Integer.class);
	}

	private int personCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM person", Integer.class);
	}

	private int claimCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reimbursement_claim", Integer.class);
	}

	private int paymentCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM reimbursement_payment", Integer.class);
	}

	private void insertUser(UUID id, String email) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""", id, email, "hash", "Tool User");
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FixedClockConfiguration {

		@Bean
		@Primary
		Clock fixedFinancialClock() {
			return Clock.fixed(
					Instant.parse("2026-08-15T03:00:00Z"),
					ZoneId.of("America/Sao_Paulo"));
		}
	}
}

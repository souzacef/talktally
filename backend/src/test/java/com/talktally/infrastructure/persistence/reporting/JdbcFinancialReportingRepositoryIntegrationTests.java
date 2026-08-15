package com.talktally.infrastructure.persistence.reporting;

import com.talktally.application.reporting.FinancialReportingRepository;
import com.talktally.application.reporting.GetCategoryBreakdownUseCase;
import com.talktally.application.reporting.GetFinancialSummaryUseCase;
import com.talktally.application.reporting.GetMonthlyCashFlowUseCase;
import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JdbcFinancialReportingRepositoryIntegrationTests {

	private static final UserId USER_A =
			UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000041"));
	private static final UserId USER_B =
			UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000042"));
	private static final CategoryId SALARY =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000001"));
	private static final CategoryId GROCERIES =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final CategoryId SHOPPING =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000011"));
	private static final CategoryId REIMBURSEMENT =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000014"));
	private static final LocalDate AUGUST_14 = LocalDate.of(2026, 8, 14);

	@Autowired
	private FinancialTransactionRepository transactionRepository;

	@Autowired
	private FinancialReportingRepository reportingRepository;

	@Autowired
	private GetFinancialSummaryUseCase summaryUseCase;

	@Autowired
	private GetCategoryBreakdownUseCase categoryUseCase;

	@Autowired
	private GetMonthlyCashFlowUseCase monthlyUseCase;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void insertUsers() {
		insertUser(USER_A, "report-a@example.test");
		insertUser(USER_B, "report-b@example.test");
	}

	@Test
	void summarySqlSeparatesKindsUsesExactDecimalsAndIsOwnerScoped() {
		saveSingle(USER_A, TransactionKind.INCOME, "1000.00", SALARY, AUGUST_14);
		saveSingle(USER_A, TransactionKind.EXPENSE, "174.00", GROCERIES, AUGUST_14);
		saveSingle(
				USER_A,
				TransactionKind.REIMBURSEMENT_RECEIPT,
				"50.00",
				REIMBURSEMENT,
				AUGUST_14.plusDays(6));
		saveSingle(USER_B, TransactionKind.INCOME, "9000.00", SALARY, AUGUST_14);

		var raw = reportingRepository.summarize(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		var summary = summaryUseCase.execute(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31));
		var incomeCategories = reportingRepository.categoryBreakdown(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31),
				TransactionKind.INCOME);

		assertEquals(new BigDecimal("1000.00"), raw.earnedIncome());
		assertEquals(new BigDecimal("174.00"), raw.expenses());
		assertEquals(new BigDecimal("50.00"), raw.reimbursementsReceived());
		assertEquals(3, raw.occurrenceCount());
		assertEquals(3, raw.transactionCount());
		assertEquals(new BigDecimal("876.00"), summary.period().netCashFlow());
		assertEquals(1, incomeCategories.size());
		assertEquals(new BigDecimal("1000.00"), incomeCategories.getFirst().total());
	}

	@Test
	void installmentReportingUsesEffectiveDatesAcrossSummaryCategoryAndMonths() {
		FinancialTransaction installment = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"Laptop",
				Money.brl(new BigDecimal("100.00")),
				SHOPPING,
				AUGUST_14,
				TransactionSource.MANUAL,
				3,
				AUGUST_14);
		transactionRepository.save(installment);

		var september = reportingRepository.summarize(
				USER_A,
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30));
		var full = reportingRepository.summarize(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 10, 31));
		var category = categoryUseCase.execute(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 10, 31),
				TransactionKind.EXPENSE);
		var monthly = monthlyUseCase.execute(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 10, 31));

		assertEquals(new BigDecimal("33.33"), september.expenses());
		assertEquals(1, september.occurrenceCount());
		assertEquals(1, september.transactionCount());
		assertEquals(new BigDecimal("100.00"), full.expenses());
		assertEquals(3, full.occurrenceCount());
		assertEquals(1, full.transactionCount());
		assertEquals(new BigDecimal("100.00"), category.total());
		assertEquals(3, category.categories().getFirst().occurrenceCount());
		assertEquals(1, category.categories().getFirst().transactionCount());
		assertEquals(new BigDecimal("33.33"), monthly.buckets().get(0).expenses());
		assertEquals(new BigDecimal("33.33"), monthly.buckets().get(1).expenses());
		assertEquals(new BigDecimal("33.34"), monthly.buckets().get(2).expenses());
	}

	@Test
	void categorySqlAggregatesWithoutJoinDuplicationAndOrdersByTotalThenCode() {
		saveSingle(USER_A, TransactionKind.EXPENSE, "20.10", GROCERIES, AUGUST_14);
		saveSingle(USER_A, TransactionKind.EXPENSE, "20.10", GROCERIES, AUGUST_14);
		saveSingle(USER_A, TransactionKind.EXPENSE, "10.05", SHOPPING, AUGUST_14);
		saveSingle(USER_B, TransactionKind.EXPENSE, "999.99", GROCERIES, AUGUST_14);

		var breakdown = reportingRepository.categoryBreakdown(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31),
				TransactionKind.EXPENSE);

		assertEquals(2, breakdown.size());
		assertEquals("GROCERIES", breakdown.getFirst().categoryCode());
		assertEquals(new BigDecimal("40.20"), breakdown.getFirst().total());
		assertEquals(2, breakdown.getFirst().occurrenceCount());
		assertEquals(2, breakdown.getFirst().transactionCount());
		assertEquals(new BigDecimal("10.05"), breakdown.get(1).total());
	}

	@Test
	void laterReimbursementDoesNotReduceHistoricalExpenseCategory() {
		saveSingle(USER_A, TransactionKind.EXPENSE, "174.00", GROCERIES, AUGUST_14);
		saveSingle(
				USER_A,
				TransactionKind.REIMBURSEMENT_RECEIPT,
				"174.00",
				REIMBURSEMENT,
				LocalDate.of(2026, 9, 14));

		var augustExpense = reportingRepository.categoryBreakdown(
				USER_A,
				LocalDate.of(2026, 8, 1),
				LocalDate.of(2026, 8, 31),
				TransactionKind.EXPENSE);
		var september = reportingRepository.summarize(
				USER_A,
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 9, 30));

		assertEquals(new BigDecimal("174.00"), augustExpense.getFirst().total());
		assertEquals(new BigDecimal("0.00"), september.expenses());
		assertEquals(new BigDecimal("174.00"), september.reimbursementsReceived());
		assertEquals(new BigDecimal("0.00"), september.earnedIncome());
	}

	private void saveSingle(
			UserId owner,
			TransactionKind kind,
			String amount,
			CategoryId category,
			LocalDate effectiveDate) {
		transactionRepository.save(FinancialTransaction.createSingleOccurrence(
				owner,
				kind,
				kind.name(),
				Money.brl(new BigDecimal(amount)),
				category,
				effectiveDate,
				TransactionSource.MANUAL));
	}

	private void insertUser(UserId userId, String email) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				userId.value(),
				email,
				"test-only-password-hash",
				email);
	}
}

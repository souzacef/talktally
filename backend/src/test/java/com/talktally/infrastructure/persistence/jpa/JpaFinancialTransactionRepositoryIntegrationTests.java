package com.talktally.infrastructure.persistence.jpa;

import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaFinancialTransactionRepositoryIntegrationTests {

	private static final UserId USER_A = UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000001"));
	private static final UserId USER_B = UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000002"));
	private static final UUID SALARY_CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
	private static final UUID GROCERIES_CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000004");
	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);

	@Autowired
	private FinancialTransactionRepository repository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Flyway flyway;

	@Autowired
	private Environment environment;

	@BeforeEach
	void insertUsers() {
		insertUser(USER_A, "user-a@example.test", "User A");
		insertUser(USER_B, "user-b@example.test", "User B");
	}

	@Test
	void flywayCreatesSchemaSeedsCategoriesAndHibernateValidatesIt() {
		assertEquals("1", flyway.info().current().getVersion().toString());
		assertEquals(15, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM category WHERE built_in = TRUE",
				Integer.class));
		assertEquals(SALARY_CATEGORY_ID, categoryId("SALARY").value());
		assertEquals(GROCERIES_CATEGORY_ID, categoryId("GROCERIES").value());
		assertEquals("REIMBURSEMENT_RECEIPT", jdbcTemplate.queryForObject(
				"SELECT allowed_kind FROM category WHERE code = 'REIMBURSEMENT'",
				String.class));
		assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
	}

	@Test
	void savesAndReloadsSingleOccurrenceExpenseLosslessly() {
		FinancialTransaction original = FinancialTransaction.createSingleOccurrence(
				USER_A,
				TransactionKind.EXPENSE,
				"Weekly groceries",
				Money.brl(new BigDecimal("87.45")),
				categoryId("GROCERIES"),
				EVENT_DATE,
				TransactionSource.MANUAL);

		FinancialTransaction saved = repository.save(original);
		FinancialTransaction loaded = repository.findById(USER_A, original.id()).orElseThrow();

		assertFinanciallyEqual(original, saved);
		assertFinanciallyEqual(original, loaded);
		assertEquals(Currency.getInstance("BRL"), loaded.totalAmount().currency());
		assertEquals(EVENT_DATE, loaded.eventDate());
		assertEquals(EVENT_DATE, loaded.occurrences().getFirst().effectiveDate());
	}

	@Test
	void savesAndReloadsInstallmentsInSequenceWithExactRemainder() {
		LocalDate firstEffectiveDate = LocalDate.of(2026, 9, 10);
		FinancialTransaction original = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"Three-installment purchase",
				Money.brl(new BigDecimal("100.00")),
				categoryId("SHOPPING"),
				EVENT_DATE,
				TransactionSource.ASSISTANT_TEXT,
				3,
				firstEffectiveDate);

		repository.save(original);
		FinancialTransaction loaded = repository.findById(USER_A, original.id()).orElseThrow();

		assertFinanciallyEqual(original, loaded);
		assertEquals(
				List.of(
						Money.brl(new BigDecimal("33.33")),
						Money.brl(new BigDecimal("33.33")),
						Money.brl(new BigDecimal("33.34"))),
				loaded.occurrences().stream().map(occurrence -> occurrence.amount()).toList());
		assertEquals(
				List.of(
						firstEffectiveDate,
						firstEffectiveDate.plusMonths(1),
						firstEffectiveDate.plusMonths(2)),
				loaded.occurrences().stream().map(occurrence -> occurrence.effectiveDate()).toList());
	}

	@Test
	void ownerScopedLookupTreatsAnotherUsersTransactionAsAbsent() {
		FinancialTransaction transaction = createExpense(USER_A, "Owner A expense");
		repository.save(transaction);

		assertTrue(repository.findById(USER_A, transaction.id()).isPresent());
		assertFalse(repository.findById(USER_B, transaction.id()).isPresent());
	}

	@Test
	void transactionsForDifferentUsersRemainIsolated() {
		FinancialTransaction userATransaction = createExpense(USER_A, "User A groceries");
		FinancialTransaction userBTransaction = FinancialTransaction.createSingleOccurrence(
				USER_B,
				TransactionKind.INCOME,
				"User B salary",
				Money.brl(new BigDecimal("5000.00")),
				categoryId("SALARY"),
				EVENT_DATE,
				TransactionSource.MANUAL);

		repository.save(userATransaction);
		repository.save(userBTransaction);

		assertTrue(repository.findById(USER_A, userATransaction.id()).isPresent());
		assertTrue(repository.findById(USER_B, userBTransaction.id()).isPresent());
		assertFalse(repository.findById(USER_A, userBTransaction.id()).isPresent());
		assertFalse(repository.findById(USER_B, userATransaction.id()).isPresent());
	}

	@Test
	void savingSameAggregateAgainReplacesRatherThanDuplicatesOccurrences() {
		FinancialTransaction transaction = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"Repeat save",
				Money.brl(new BigDecimal("100.00")),
				categoryId("SHOPPING"),
				EVENT_DATE,
				TransactionSource.MANUAL,
				3,
				EVENT_DATE);

		repository.save(transaction);
		repository.save(transaction);

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM financial_transaction WHERE id = ?",
				Integer.class,
				transaction.id().value()));
		assertEquals(3, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM transaction_occurrence WHERE transaction_id = ?",
				Integer.class,
				transaction.id().value()));
		assertFinanciallyEqual(
				transaction,
				repository.findById(USER_A, transaction.id()).orElseThrow());
	}

	@Test
	void compositeForeignKeyRejectsOccurrenceWithDifferentOwner() {
		FinancialTransaction transaction = createExpense(USER_A, "Ownership constraint");
		repository.save(transaction);

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO transaction_occurrence
				    (id, transaction_id, user_id, sequence_number, effective_date, amount, currency)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				transaction.id().value(),
				USER_B.value(),
				2,
				EVENT_DATE.plusMonths(1),
				new BigDecimal("1.00"),
				"BRL"));
	}

	private void insertUser(UserId userId, String email, String displayName) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				userId.value(),
				email,
				"test-only-password-hash",
				displayName);
	}

	private CategoryId categoryId(String code) {
		UUID value = jdbcTemplate.queryForObject(
				"SELECT id FROM category WHERE code = ?",
				UUID.class,
				code);
		return CategoryId.from(value);
	}

	private FinancialTransaction createExpense(UserId ownerId, String description) {
		return FinancialTransaction.createSingleOccurrence(
				ownerId,
				TransactionKind.EXPENSE,
				description,
				Money.brl(new BigDecimal("25.00")),
				categoryId("GROCERIES"),
				EVENT_DATE,
				TransactionSource.MANUAL);
	}

	private static void assertFinanciallyEqual(
			FinancialTransaction expected,
			FinancialTransaction actual) {
		assertEquals(expected.id(), actual.id());
		assertEquals(expected.ownerId(), actual.ownerId());
		assertEquals(expected.kind(), actual.kind());
		assertEquals(expected.description(), actual.description());
		assertEquals(expected.totalAmount(), actual.totalAmount());
		assertEquals(expected.categoryId(), actual.categoryId());
		assertEquals(expected.eventDate(), actual.eventDate());
		assertEquals(expected.source(), actual.source());
		assertEquals(expected.occurrences(), actual.occurrences());
	}
}

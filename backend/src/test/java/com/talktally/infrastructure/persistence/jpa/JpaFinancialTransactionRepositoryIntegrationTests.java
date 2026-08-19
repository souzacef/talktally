package com.talktally.infrastructure.persistence.jpa;

import com.talktally.domain.CategoryCatalog;
import com.talktally.domain.CategoryId;
import com.talktally.domain.CategoryMetadata;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionPage;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.FinancialTransactionSearchCriteria;
import com.talktally.domain.Money;
import com.talktally.domain.TransactionId;
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
import java.util.Comparator;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
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
	private static final UUID OTHER_CATEGORY_ID = UUID.fromString("00000000-0000-0000-0000-000000000015");
	private static final CategoryId SHOPPING_CATEGORY = CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000011"));
	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);

	@Autowired
	private FinancialTransactionRepository repository;

	@Autowired
	private CategoryCatalog categoryCatalog;

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
		assertEquals("2", flyway.info().current().getVersion().toString());
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
		assertEquals(EVENT_DATE, loaded.firstOccurrenceDate());
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
		assertEquals(firstEffectiveDate, loaded.firstOccurrenceDate());
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

	@Test
	void filteredSearchIsOwnerScopedAndAppliesKindAndCategoryInJpa() {
		FinancialTransaction matching = createExpense(USER_A, "A groceries");
		FinancialTransaction otherCategory = FinancialTransaction.createSingleOccurrence(
				USER_A,
				TransactionKind.EXPENSE,
				"A shopping",
				Money.brl(new BigDecimal("30.00")),
				SHOPPING_CATEGORY,
				EVENT_DATE,
				TransactionSource.MANUAL);
		FinancialTransaction otherKind = createIncome(USER_A, "A salary");
		FinancialTransaction otherOwner = createExpense(USER_B, "B groceries");
		repository.save(matching);
		repository.save(otherCategory);
		repository.save(otherKind);
		repository.save(otherOwner);

		FinancialTransactionPage result = repository.search(
				USER_A,
				criteria(
						TransactionKind.EXPENSE,
						categoryId("GROCERIES"),
						null,
						null,
						"grocer",
						0,
						20));

		assertEquals(1, result.totalElements());
		assertEquals(matching.id(), result.content().getFirst().id());
	}

	@Test
	void effectiveDateSearchUsesOccurrenceExistenceAndReturnsInstallmentOnce() {
		FinancialTransaction installment = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"Three months",
				Money.brl(new BigDecimal("90.00")),
				SHOPPING_CATEGORY,
				EVENT_DATE,
				TransactionSource.MANUAL,
				3,
				EVENT_DATE);
		FinancialTransaction outside = FinancialTransaction.createSingleOccurrence(
				USER_A,
				TransactionKind.EXPENSE,
				"Outside",
				Money.brl(new BigDecimal("10.00")),
				categoryId("GROCERIES"),
				EVENT_DATE.minusMonths(2),
				TransactionSource.MANUAL);
		repository.save(installment);
		repository.save(outside);

		FinancialTransactionPage september = repository.search(
				USER_A,
				criteria(
						null,
						null,
						LocalDate.of(2026, 9, 1),
						LocalDate.of(2026, 10, 31),
						null,
						0,
						20));

		assertEquals(1, september.totalElements());
		assertEquals(1, september.content().size());
		assertEquals(installment.id(), september.content().getFirst().id());
		assertEquals(3, september.content().getFirst().occurrences().size());
	}

	@Test
	void paginationUsesEventDateDescendingThenStableIdOrdering() {
		FinancialTransaction first = createExpense(USER_A, "First id");
		FinancialTransaction second = createExpense(USER_A, "Second id");
		FinancialTransaction third = createExpense(USER_A, "Third id");
		List<FinancialTransaction> expected = List.of(first, second, third).stream()
				.sorted(Comparator.comparing(
						transaction -> transaction.id().value().toString()))
				.toList();
		repository.save(first);
		repository.save(second);
		repository.save(third);

		FinancialTransactionPage pageZero = repository.search(
				USER_A, criteria(null, null, null, null, null, 0, 1));
		FinancialTransactionPage pageOne = repository.search(
				USER_A, criteria(null, null, null, null, null, 1, 1));
		FinancialTransactionPage pageTwo = repository.search(
				USER_A, criteria(null, null, null, null, null, 2, 1));

		assertEquals(expected.get(0).id(), pageZero.content().getFirst().id());
		assertEquals(expected.get(1).id(), pageOne.content().getFirst().id());
		assertEquals(expected.get(2).id(), pageTwo.content().getFirst().id());
		assertEquals(3, pageZero.totalElements());
		assertEquals(3, pageZero.totalPages());
	}

	@Test
	void ownerScopedDeleteRemovesHeaderAndOccurrencesByDatabaseCascade() {
		FinancialTransaction transaction = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"Delete installments",
				Money.brl(new BigDecimal("90.00")),
				SHOPPING_CATEGORY,
				EVENT_DATE,
				TransactionSource.MANUAL,
				3,
				EVENT_DATE);
		repository.save(transaction);

		assertTrue(repository.deleteById(USER_A, transaction.id()));
		assertEquals(0, countRows("financial_transaction", transaction.id()));
		assertEquals(0, countRows("transaction_occurrence", transaction.id()));
	}

	@Test
	void crossOwnerDeleteUsesOwnershipPredicateAndChangesNothing() {
		FinancialTransaction transaction = createExpense(USER_A, "Owner only");
		repository.save(transaction);

		assertFalse(repository.deleteById(USER_B, transaction.id()));
		assertEquals(1, countRows("financial_transaction", transaction.id()));
		assertEquals(1, countRows("transaction_occurrence", transaction.id()));
		assertTrue(repository.findById(USER_A, transaction.id()).isPresent());
	}

	@Test
	void categoryCatalogUsesSeededCompatibilityAndOwnerVisibility() {
		CategoryId salaryId = CategoryId.from(SALARY_CATEGORY_ID);
		CategoryId groceriesId = CategoryId.from(GROCERIES_CATEGORY_ID);
		CategoryId otherId = CategoryId.from(OTHER_CATEGORY_ID);
		CategoryId reimbursementId = categoryId("REIMBURSEMENT");
		CategoryId customId = CategoryId.from(UUID.fromString("30000000-0000-0000-0000-000000000001"));
		insertCustomCategory(customId, USER_A);

		CategoryMetadata salaryForA = categoryCatalog.findVisibleById(USER_A, salaryId).orElseThrow();
		CategoryMetadata salaryForB = categoryCatalog.findVisibleById(USER_B, salaryId).orElseThrow();
		CategoryMetadata groceries = categoryCatalog.findVisibleById(USER_A, groceriesId).orElseThrow();
		CategoryMetadata other = categoryCatalog.findVisibleById(USER_B, otherId).orElseThrow();
		CategoryMetadata reimbursement = categoryCatalog
				.findVisibleById(USER_A, reimbursementId).orElseThrow();

		assertAll(
				() -> assertTrue(salaryForA.allows(TransactionKind.INCOME)),
				() -> assertFalse(salaryForA.allows(TransactionKind.EXPENSE)),
				() -> assertEquals(salaryForA, salaryForB),
				() -> assertTrue(groceries.allows(TransactionKind.EXPENSE)),
				() -> assertFalse(groceries.allows(TransactionKind.INCOME)),
				() -> assertTrue(other.allows(TransactionKind.INCOME)),
				() -> assertTrue(other.allows(TransactionKind.EXPENSE)),
				() -> assertTrue(reimbursement.allows(TransactionKind.REIMBURSEMENT_RECEIPT)),
				() -> assertFalse(reimbursement.allows(TransactionKind.EXPENSE)),
				() -> assertTrue(categoryCatalog.findVisibleById(USER_A, customId).isPresent()),
				() -> assertFalse(categoryCatalog.findVisibleById(USER_B, customId).isPresent()));
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

	private void insertCustomCategory(CategoryId categoryId, UserId ownerId) {
		jdbcTemplate.update("""
				INSERT INTO category
				    (id, owner_user_id, code, display_name, allowed_kind, built_in)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				categoryId.value(),
				ownerId.value(),
				"USER_A_CUSTOM",
				"User A custom",
				"EXPENSE",
				false);
	}

	private int countRows(String table, TransactionId transactionId) {
		String idColumn = table.equals("financial_transaction") ? "id" : "transaction_id";
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + table + " WHERE " + idColumn + " = ?",
				Integer.class,
				transactionId.value());
	}

	private static FinancialTransactionSearchCriteria criteria(
			TransactionKind kind,
			CategoryId categoryId,
			LocalDate from,
			LocalDate to,
			String searchText,
			int page,
			int size) {
		return new FinancialTransactionSearchCriteria(
				Optional.ofNullable(kind),
				Optional.ofNullable(categoryId),
				Optional.ofNullable(from),
				Optional.ofNullable(to),
				Optional.ofNullable(searchText),
				page,
				size);
	}

	private FinancialTransaction createIncome(UserId ownerId, String description) {
		return FinancialTransaction.createSingleOccurrence(
				ownerId,
				TransactionKind.INCOME,
				description,
				Money.brl(new BigDecimal("5000.00")),
				categoryId("SALARY"),
				EVENT_DATE,
				TransactionSource.MANUAL);
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

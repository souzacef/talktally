package com.talktally.infrastructure.persistence;

import com.talktally.application.auth.EmailPolicy;
import com.talktally.application.auth.exception.DuplicateEmailException;
import com.talktally.application.auth.port.PasswordHasher;
import com.talktally.application.exception.ProtectedTransactionException;
import com.talktally.application.person.CreatePersonUseCase;
import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.application.reimbursement.CreateReimbursableExpenseUseCase;
import com.talktally.application.reimbursement.RecordReimbursementPaymentUseCase;
import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.application.reporting.FinancialReportingRepository;
import com.talktally.application.reporting.GetFinancialSummaryUseCase;
import com.talktally.application.transaction.DeleteTransactionUseCase;
import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionPage;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.FinancialTransactionSearchCriteria;
import com.talktally.domain.Money;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserAccount;
import com.talktally.domain.UserAccountRepository;
import com.talktally.domain.UserId;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
		"spring.ai.model.chat=none",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.flyway.enabled=true",
		"talktally.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@Transactional
class PostgreSqlProductionIntegrationTests {

	private static final String POSTGRES_IMAGE = "postgres:17.6-alpine";
	private static final UserId USER_A =
			UserId.from(UUID.fromString("20000000-0000-0000-0000-000000000001"));
	private static final UserId USER_B =
			UserId.from(UUID.fromString("20000000-0000-0000-0000-000000000002"));
	private static final CategoryId SALARY =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000001"));
	private static final CategoryId GROCERIES =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final CategoryId SHOPPING =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000011"));
	private static final CategoryId REIMBURSEMENT =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000014"));
	private static final LocalDate AUGUST_14 = LocalDate.of(2026, 8, 14);

	@Container
	private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
			DockerImageName.parse(POSTGRES_IMAGE));

	@DynamicPropertySource
	static void postgresProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
		registry.add("spring.datasource.username", POSTGRES::getUsername);
		registry.add("spring.datasource.password", POSTGRES::getPassword);
		registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
	}

	@Autowired
	private DataSource dataSource;

	@Autowired
	private Flyway flyway;

	@Autowired
	private Environment environment;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private FinancialTransactionRepository transactionRepository;

	@Autowired
	private FinancialReportingRepository reportingRepository;

	@Autowired
	private GetFinancialSummaryUseCase summaryUseCase;

	@Autowired
	private CreatePersonUseCase createPersonUseCase;

	@Autowired
	private CreateReimbursableExpenseUseCase createExpenseUseCase;

	@Autowired
	private RecordReimbursementPaymentUseCase paymentUseCase;

	@Autowired
	private DeleteTransactionUseCase deleteTransactionUseCase;

	@Autowired
	private UserAccountRepository userAccountRepository;

	@Autowired
	private PasswordHasher passwordHasher;

	@BeforeEach
	void insertOwners() {
		insertUser(USER_A, "postgres-a@example.test", "User A");
		insertUser(USER_B, "postgres-b@example.test", "User B");
	}

	@Test
	void contextStartsWithFlywayV1AndV2AndHibernateValidationOnPostgreSql()
			throws SQLException {
		flyway.validate();

		try (Connection connection = dataSource.getConnection()) {
			assertTrue(connection.getMetaData()
					.getDatabaseProductName()
					.contains("PostgreSQL"));
		}
		assertEquals("2", flyway.info().current().getVersion().toString());
		assertEquals(
				List.of("1:true", "2:true"),
				jdbcTemplate.query(
						"""
						SELECT version, success
						FROM flyway_schema_history
						WHERE type = 'SQL'
						ORDER BY installed_rank
						""",
						(resultSet, row) -> resultSet.getString("version")
								+ ":" + resultSet.getBoolean("success")));
		assertEquals("validate", environment.getProperty("spring.jpa.hibernate.ddl-auto"));
		assertEquals("uuid", columnType("financial_transaction", "id"));
		assertEquals("numeric", columnType("financial_transaction", "total_amount"));
		assertEquals("bpchar", columnType("financial_transaction", "currency"));
		assertEquals(15, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM category WHERE built_in = TRUE", Integer.class));
	}

	@Test
	void transactionPersistencePreservesUuidBrlDecimalsOwnershipAndFutureOccurrences() {
		LocalDate firstOccurrence = LocalDate.of(2026, 9, 10);
		FinancialTransaction installment = FinancialTransaction.createInstallment(
				USER_A,
				TransactionKind.EXPENSE,
				"PostgreSQL installment",
				Money.brl(new BigDecimal("100.00")),
				SHOPPING,
				AUGUST_14,
				TransactionSource.MANUAL,
				3,
				firstOccurrence);

		transactionRepository.save(installment);
		FinancialTransaction loaded = transactionRepository.findById(
				USER_A, installment.id()).orElseThrow();

		assertEquals(installment.id().value(), jdbcTemplate.queryForObject(
				"SELECT id FROM financial_transaction WHERE id = ?",
				UUID.class,
				installment.id().value()));
		assertEquals(new BigDecimal("100.00"), loaded.totalAmount().amount());
		assertEquals(Currency.getInstance("BRL"), loaded.totalAmount().currency());
		assertEquals("BRL", jdbcTemplate.queryForObject(
				"SELECT currency FROM financial_transaction WHERE id = ?",
				String.class,
				installment.id().value()));
		assertEquals(
				List.of(
						new BigDecimal("33.33"),
						new BigDecimal("33.33"),
						new BigDecimal("33.34")),
				loaded.occurrences().stream()
						.map(occurrence -> occurrence.amount().amount())
						.toList());
		assertFalse(transactionRepository.findById(USER_B, installment.id()).isPresent());

		FinancialTransactionPage future = transactionRepository.search(
				USER_A,
				new FinancialTransactionSearchCriteria(
						Optional.empty(),
						Optional.empty(),
						Optional.of(firstOccurrence.plusMonths(2)),
						Optional.of(firstOccurrence.plusMonths(2)),
						Optional.empty(),
						0,
						20));
		assertEquals(1, future.totalElements());
		assertEquals(installment.id(), future.content().getFirst().id());

		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO transaction_occurrence
				    (id, transaction_id, user_id, sequence_number, effective_date, amount, currency)
				VALUES (?, ?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				installment.id().value(),
				USER_B.value(),
				4,
				firstOccurrence.plusMonths(3),
				new BigDecimal("1.00"),
				"BRL"));
	}

	@Test
	void reimbursementFlowPersistsReceiptProtectsLinksAndEnforcesCompositeOwnership() {
		var person = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("PostgreSQL Person"));
		var created = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				new CreateReimbursableExpenseInput(
						"Reimbursable dinner",
						new BigDecimal("174.00"),
						GROCERIES,
						AUGUST_14,
						null,
						1,
						person.personId(),
						new BigDecimal("174.00"),
						"Business meal"));
		var partial = paymentUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				created.claim().claimId(),
				new RecordReimbursementPaymentInput(
						new BigDecimal("50.00"), AUGUST_14.plusDays(6), "Partial"));

		FinancialTransaction receipt = transactionRepository.findById(
				USER_A, partial.receiptTransactionId()).orElseThrow();
		assertEquals(TransactionKind.REIMBURSEMENT_RECEIPT, receipt.kind());
		assertEquals(new BigDecimal("50.00"), receipt.totalAmount().amount());
		assertEquals(REIMBURSEMENT, receipt.categoryId());
		assertEquals(ReimbursementStatus.PARTIALLY_PAID, partial.claim().status());
		assertEquals(new BigDecimal("124.00"), partial.claim().remainingAmount());
		assertThrows(
				ProtectedTransactionException.class,
				() -> deleteTransactionUseCase.execute(
						USER_A, created.expense().transactionId()));
		assertThrows(
				ProtectedTransactionException.class,
				() -> deleteTransactionUseCase.execute(
						USER_A, partial.receiptTransactionId()));

		var otherUsersPerson = createPersonUseCase.execute(
				USER_B, new CreatePersonInput("Other owner"));
		FinancialTransaction unclaimedExpense = transactionRepository.save(
				FinancialTransaction.createSingleOccurrence(
						USER_A,
						TransactionKind.EXPENSE,
						"Unclaimed expense",
						Money.brl(new BigDecimal("10.00")),
						GROCERIES,
						AUGUST_14,
						TransactionSource.MANUAL));
		assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update("""
				INSERT INTO reimbursement_claim
				    (id, user_id, expense_transaction_id, person_id, original_amount, currency)
				VALUES (?, ?, ?, ?, ?, ?)
				""",
				UUID.randomUUID(),
				USER_A.value(),
				unclaimedExpense.id().value(),
				otherUsersPerson.personId().value(),
				new BigDecimal("10.00"),
				"BRL"));
	}

	@Test
	void reportingUsesExactPostgreSqlNumericAndInstallmentMonthSemantics() {
		saveSingle(USER_A, TransactionKind.INCOME, "1000.00", SALARY, AUGUST_14);
		saveSingle(USER_A, TransactionKind.EXPENSE, "174.00", GROCERIES, AUGUST_14);
		saveSingle(
				USER_A,
				TransactionKind.REIMBURSEMENT_RECEIPT,
				"50.00",
				REIMBURSEMENT,
				AUGUST_14.plusDays(6));

		var totals = reportingRepository.summarize(
				USER_A, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
		var summary = summaryUseCase.execute(
				USER_A, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31));
		assertEquals(new BigDecimal("1000.00"), totals.earnedIncome());
		assertEquals(new BigDecimal("174.00"), totals.expenses());
		assertEquals(new BigDecimal("50.00"), totals.reimbursementsReceived());
		assertEquals(new BigDecimal("876.00"), summary.period().netCashFlow());

		transactionRepository.save(FinancialTransaction.createInstallment(
				USER_B,
				TransactionKind.EXPENSE,
				"Three reporting months",
				Money.brl(new BigDecimal("100.00")),
				SHOPPING,
				AUGUST_14,
				TransactionSource.MANUAL,
				3,
				AUGUST_14));
		var months = reportingRepository.monthlyCashFlow(
				USER_B, LocalDate.of(2026, 8, 1), LocalDate.of(2026, 10, 31));
		assertEquals(
				List.of(
						new BigDecimal("33.33"),
						new BigDecimal("33.33"),
						new BigDecimal("33.34")),
				months.stream().map(month -> month.expenses()).toList());
	}

	@Test
	void accountPersistenceRoundTripsNormalizedEmailBcryptAndCurrencyWithUniqueness() {
		String normalizedEmail = EmailPolicy.normalize("  PostgreSQL.User@Example.Test  ");
		String passwordHash = passwordHasher.hash("correct horse battery staple");
		UserAccount account = UserAccount.create(
				UserId.generate(), normalizedEmail, passwordHash, "PostgreSQL User");

		userAccountRepository.save(account);
		UserAccount loaded = userAccountRepository.findByNormalizedEmail(
				normalizedEmail).orElseThrow();

		assertEquals("postgresql.user@example.test", loaded.normalizedEmail());
		assertTrue(loaded.passwordHash().startsWith("{bcrypt}"));
		assertTrue(passwordHasher.matches(
				"correct horse battery staple", loaded.passwordHash()));
		assertEquals(Currency.getInstance("BRL"), loaded.defaultCurrency());
		assertEquals(passwordHash, jdbcTemplate.queryForObject(
				"SELECT password_hash FROM app_user WHERE id = ?",
				String.class,
				account.id().value()));
		assertThrows(
				DuplicateEmailException.class,
				() -> userAccountRepository.save(UserAccount.create(
						UserId.generate(),
						normalizedEmail,
						passwordHasher.hash("another password"),
						"Duplicate")));
	}

	private String columnType(String table, String column) {
		return jdbcTemplate.queryForObject("""
				SELECT udt_name
				FROM information_schema.columns
				WHERE table_schema = 'public' AND table_name = ? AND column_name = ?
				""", String.class, table, column);
	}

	private void saveSingle(
			UserId owner,
			TransactionKind kind,
			String amount,
			CategoryId category,
			LocalDate date) {
		transactionRepository.save(FinancialTransaction.createSingleOccurrence(
				owner,
				kind,
				kind.name(),
				Money.brl(new BigDecimal(amount)),
				category,
				date,
				TransactionSource.MANUAL));
	}

	private void insertUser(UserId userId, String email, String name) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				userId.value(),
				email,
				"{bcrypt}$2a$10$test-only-not-used",
				name);
	}
}

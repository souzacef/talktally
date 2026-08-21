package com.talktally.infrastructure.persistence;

import com.talktally.application.person.CreatePersonUseCase;
import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.application.reimbursement.CreateReimbursableExpenseUseCase;
import com.talktally.application.reimbursement.RecordReimbursementPaymentUseCase;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;
import com.talktally.domain.CategoryId;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimId;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest(properties = {
		"spring.ai.model.chat=none",
		"spring.jpa.hibernate.ddl-auto=validate",
		"spring.flyway.enabled=true",
		"talktally.security.jwt.secret-base64=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class PostgreSqlReimbursementConcurrencyIntegrationTests {

	private static final String POSTGRES_IMAGE = "postgres:17.6-alpine";
	private static final CategoryId GROCERIES = CategoryId.from(
			UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final LocalDate RECEIVED_DATE = LocalDate.of(2026, 8, 20);
	private static final int TIMEOUT_SECONDS = 20;

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
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private DataSource dataSource;

	@Autowired
	private CreatePersonUseCase createPersonUseCase;

	@Autowired
	private CreateReimbursableExpenseUseCase createExpenseUseCase;

	@Autowired
	private RecordReimbursementPaymentUseCase paymentUseCase;

	@Autowired
	private ReimbursementClaimRepository claimRepository;

	@Test
	@Timeout(value = 45, unit = TimeUnit.SECONDS)
	void concurrentOverpaymentsSerializeAndLeaveOnlyOneReceiptAndPayment() throws Exception {
		Scenario scenario = createClaim("100.00");

		List<PaymentAttempt> attempts = executeConcurrently(
				scenario, new BigDecimal("80.00"), new BigDecimal("80.00"));
		List<PaymentAttempt> successes = attempts.stream()
				.filter(PaymentAttempt::succeeded)
				.toList();
		List<RuntimeException> failures = attempts.stream()
				.filter(attempt -> !attempt.succeeded())
				.map(PaymentAttempt::failure)
				.toList();

		assertAll(
				() -> assertEquals(1, successes.size()),
				() -> assertEquals(1, failures.size()),
				() -> assertInstanceOf(
						InvalidReimbursementInputException.class,
						failures.getFirst()),
				() -> assertTrue(failures.getFirst().getMessage()
						.contains("payment total must not exceed original amount")));
		assertFinancialState(scenario, "80.00", 1, "20.00");
	}

	@Test
	@Timeout(value = 45, unit = TimeUnit.SECONDS)
	void concurrentPaymentsThatExactlyCompleteTheClaimBothSucceed() throws Exception {
		Scenario scenario = createClaim("100.00");

		List<PaymentAttempt> attempts = executeConcurrently(
				scenario, new BigDecimal("40.00"), new BigDecimal("60.00"));

		assertTrue(attempts.stream().allMatch(PaymentAttempt::succeeded));
		assertEquals(
				List.of(new BigDecimal("40.00"), new BigDecimal("60.00")),
				jdbcTemplate.queryForList("""
						SELECT amount
						FROM reimbursement_payment
						WHERE claim_id = ?
						ORDER BY amount
						""", BigDecimal.class, scenario.claimId().value()));
		assertFinancialState(scenario, "100.00", 2, "0.00");
	}

	private List<PaymentAttempt> executeConcurrently(
			Scenario scenario,
			BigDecimal firstAmount,
			BigDecimal secondAmount) throws Exception {
		CountDownLatch workersReady = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(
				2,
				Thread.ofPlatform()
						.daemon()
						.name("reimbursement-payment-worker-", 0)
						.factory());
		try {
			Future<PaymentAttempt> first;
			Future<PaymentAttempt> second;
			try (Connection lockConnection = dataSource.getConnection()) {
				lockConnection.setAutoCommit(false);
				lockClaimRow(lockConnection, scenario);
				first = executor.submit(
						paymentAttempt(scenario, firstAmount, workersReady, start));
				second = executor.submit(
						paymentAttempt(scenario, secondAmount, workersReady, start));
				assertTrue(
						workersReady.await(TIMEOUT_SECONDS, TimeUnit.SECONDS),
						"both repayment workers must be ready before release");
				start.countDown();
				awaitBlockedClaimLockRequests(2);
				assertFalse(first.isDone(), "first repayment must still be lock-blocked");
				assertFalse(second.isDone(), "second repayment must still be lock-blocked");
				lockConnection.rollback();
			}
			return List.of(
					first.get(TIMEOUT_SECONDS, TimeUnit.SECONDS),
					second.get(TIMEOUT_SECONDS, TimeUnit.SECONDS));
		}
		finally {
			start.countDown();
			executor.shutdownNow();
			assertTrue(
					executor.awaitTermination(TIMEOUT_SECONDS, TimeUnit.SECONDS),
					"repayment workers must terminate within the bounded timeout");
		}
	}

	private void lockClaimRow(Connection connection, Scenario scenario) throws Exception {
		try (PreparedStatement statement = connection.prepareStatement("""
				SELECT id
				FROM reimbursement_claim
				WHERE id = ? AND user_id = ?
				FOR UPDATE
				""")) {
			statement.setObject(1, scenario.claimId().value());
			statement.setObject(2, scenario.ownerId().value());
			try (var result = statement.executeQuery()) {
				assertTrue(result.next(), "committed reimbursement claim fixture must exist");
			}
		}
	}

	private void awaitBlockedClaimLockRequests(int expectedCount) throws Exception {
		long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
		do {
			Integer blockedCount = jdbcTemplate.queryForObject("""
					SELECT COUNT(*)
					FROM pg_stat_activity
					WHERE datname = current_database()
					  AND pid <> pg_backend_pid()
					  AND wait_event_type = 'Lock'
					  AND query ILIKE '%reimbursement_claim%'
					""", Integer.class);
			if (blockedCount != null && blockedCount >= expectedCount) {
				return;
			}
			Thread.sleep(25);
		}
		while (System.nanoTime() < deadline);
		throw new AssertionError(
				"both repayment transactions must overlap while waiting for the claim lock");
	}

	private Callable<PaymentAttempt> paymentAttempt(
			Scenario scenario,
			BigDecimal amount,
			CountDownLatch workersReady,
			CountDownLatch start) {
		return () -> {
			workersReady.countDown();
			if (!start.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				throw new IllegalStateException("concurrent repayment start was not released");
			}
			try {
				return new PaymentAttempt(
						paymentUseCase.execute(
								scenario.ownerId(),
								TransactionSource.MANUAL,
								scenario.claimId(),
								new RecordReimbursementPaymentInput(
										amount, RECEIVED_DATE, "Concurrent repayment")),
						null);
			}
			catch (RuntimeException exception) {
				return new PaymentAttempt(null, exception);
			}
		};
	}

	private Scenario createClaim(String amount) {
		UserId ownerId = UserId.generate();
		insertUser(ownerId);
		var person = createPersonUseCase.execute(
				ownerId, new CreatePersonInput("Concurrent payer"));
		var created = createExpenseUseCase.execute(
				ownerId,
				TransactionSource.MANUAL,
				new CreateReimbursableExpenseInput(
						"Concurrent reimbursable expense",
						new BigDecimal(amount),
						GROCERIES,
						RECEIVED_DATE.minusDays(1),
						null,
						1,
						person.personId(),
						new BigDecimal(amount),
						null));
		return new Scenario(ownerId, created.claim().claimId());
	}

	private void assertFinancialState(
			Scenario scenario,
			String expectedPaymentTotal,
			int expectedPaymentCount,
			String expectedRemainingAmount) {
		UUID claimId = scenario.claimId().value();
		UUID ownerId = scenario.ownerId().value();
		ReimbursementClaim claim = claimRepository.findById(
				scenario.ownerId(), scenario.claimId()).orElseThrow();

		assertAll(
				() -> assertEquals(new BigDecimal(expectedPaymentTotal), jdbcTemplate.queryForObject(
						"SELECT COALESCE(SUM(amount), 0) FROM reimbursement_payment WHERE claim_id = ?",
						BigDecimal.class,
						claimId)),
				() -> assertEquals(expectedPaymentCount, jdbcTemplate.queryForObject(
						"SELECT COUNT(*) FROM reimbursement_payment WHERE claim_id = ?",
						Integer.class,
						claimId)),
				() -> assertEquals(expectedPaymentCount, jdbcTemplate.queryForObject("""
						SELECT COUNT(*)
						FROM financial_transaction
						WHERE user_id = ? AND kind = 'REIMBURSEMENT_RECEIPT'
						""", Integer.class, ownerId)),
				() -> assertEquals(0, jdbcTemplate.queryForObject("""
						SELECT COUNT(*)
						FROM reimbursement_payment payment
						LEFT JOIN financial_transaction receipt
						  ON receipt.id = payment.receipt_transaction_id
						 AND receipt.user_id = payment.user_id
						WHERE payment.claim_id = ? AND receipt.id IS NULL
						""", Integer.class, claimId)),
				() -> assertEquals(0, jdbcTemplate.queryForObject("""
						SELECT COUNT(*)
						FROM financial_transaction receipt
						LEFT JOIN reimbursement_payment payment
						  ON payment.receipt_transaction_id = receipt.id
						 AND payment.user_id = receipt.user_id
						WHERE receipt.user_id = ?
						  AND receipt.kind = 'REIMBURSEMENT_RECEIPT'
						  AND payment.id IS NULL
						""", Integer.class, ownerId)),
				() -> assertEquals(
						new BigDecimal(expectedRemainingAmount),
						claim.remainingAmount().amount()));
	}

	private void insertUser(UserId userId) {
		jdbcTemplate.update("""
				INSERT INTO app_user (id, email, password_hash, display_name)
				VALUES (?, ?, ?, ?)
				""",
				userId.value(),
				"concurrency-" + userId.value() + "@example.test",
				"{bcrypt}$2a$10$test-only-not-used",
				"Concurrency User");
	}

	private record Scenario(UserId ownerId, ReimbursementClaimId claimId) {
	}

	private record PaymentAttempt(
			RecordReimbursementPaymentOutput output,
			RuntimeException failure) {

		boolean succeeded() {
			return output != null;
		}
	}
}

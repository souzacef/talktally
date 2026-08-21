package com.talktally.infrastructure.persistence.jpa;

import com.talktally.application.person.CreatePersonUseCase;
import com.talktally.application.person.GetPersonReimbursementSummaryUseCase;
import com.talktally.application.person.exception.DuplicatePersonException;
import com.talktally.application.person.input.CreatePersonInput;
import com.talktally.application.person.output.PersonOutput;
import com.talktally.application.reimbursement.CreateReimbursableExpenseUseCase;
import com.talktally.application.reimbursement.ListReimbursementsUseCase;
import com.talktally.application.reimbursement.RecordReimbursementPaymentUseCase;
import com.talktally.application.reimbursement.exception.InvalidReimbursementInputException;
import com.talktally.application.reimbursement.input.CreateReimbursableExpenseInput;
import com.talktally.application.reimbursement.input.ListReimbursementsInput;
import com.talktally.application.reimbursement.input.RecordReimbursementPaymentInput;
import com.talktally.application.reimbursement.output.CreateReimbursableExpenseOutput;
import com.talktally.application.reimbursement.output.RecordReimbursementPaymentOutput;
import com.talktally.domain.CategoryId;
import com.talktally.domain.FinancialTransaction;
import com.talktally.domain.FinancialTransactionRepository;
import com.talktally.domain.Money;
import com.talktally.domain.Person;
import com.talktally.domain.PersonId;
import com.talktally.domain.PersonRepository;
import com.talktally.domain.ReimbursementClaim;
import com.talktally.domain.ReimbursementClaimRepository;
import com.talktally.domain.ReimbursementStatus;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;
import com.talktally.domain.UserId;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.TestTransaction;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class JpaReimbursementRepositoryIntegrationTests {

	private static final UserId USER_A =
			UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000021"));
	private static final UserId USER_B =
			UserId.from(UUID.fromString("10000000-0000-0000-0000-000000000022"));
	private static final CategoryId GROCERIES =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000004"));
	private static final CategoryId REIMBURSEMENT =
			CategoryId.from(UUID.fromString("00000000-0000-0000-0000-000000000014"));
	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);

	@Autowired
	private Flyway flyway;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PersonRepository personRepository;

	@Autowired
	private ReimbursementClaimRepository claimRepository;

	@Autowired
	private FinancialTransactionRepository transactionRepository;

	@Autowired
	private CreatePersonUseCase createPersonUseCase;

	@Autowired
	private CreateReimbursableExpenseUseCase createExpenseUseCase;

	@Autowired
	private RecordReimbursementPaymentUseCase paymentUseCase;

	@Autowired
	private ListReimbursementsUseCase listUseCase;

	@Autowired
	private GetPersonReimbursementSummaryUseCase summaryUseCase;

	@BeforeEach
	void insertUsers() {
		insertUser(USER_A, "reimbursement-a@example.test");
		insertUser(USER_B, "reimbursement-b@example.test");
	}

	@Test
	void flywayV2AppliesAndPeopleAreOwnerScopedAndNormalizedUnique() {
		assertEquals("2", flyway.info().current().getVersion().toString());
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("  Jon   Doe  "));

		assertEquals("Jon   Doe", jon.displayName());
		assertTrue(personRepository.findByNormalizedName(USER_A, "jon doe").isPresent());
		assertFalse(personRepository.findById(USER_B, jon.personId()).isPresent());
		assertThrows(
				DuplicatePersonException.class,
				() -> createPersonUseCase.execute(
						USER_A, new CreatePersonInput("JON DOE")));
		PersonOutput otherUsersJon = createPersonUseCase.execute(
				USER_B, new CreatePersonInput("Jon Doe"));
		assertEquals("Jon Doe", otherUsersJon.displayName());
	}

	@Test
	void canonicalFlowRoundTripsDerivesStatusUsesTrustedSourcesAndSummarizes() {
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Jon Doe"));
		CreateReimbursableExpenseOutput created = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.ASSISTANT_TEXT,
				expenseInput(jon.personId(), new BigDecimal("174.00"), null, "Dinner"));

		assertEquals(TransactionKind.EXPENSE, created.expense().kind());
		assertEquals(TransactionSource.ASSISTANT_TEXT, created.expense().source());
		assertEquals(3, created.expense().occurrences().size());
		assertEquals(
				new BigDecimal("58.00"),
				created.expense().occurrences().getFirst().amount());
		assertEquals(ReimbursementStatus.PENDING, created.claim().status());
		assertEquals(new BigDecimal("174.00"), created.claim().remainingAmount());

		RecordReimbursementPaymentOutput partial = paymentUseCase.execute(
				USER_A,
				TransactionSource.VOICE,
				created.claim().claimId(),
				new RecordReimbursementPaymentInput(
						new BigDecimal("50.00"), EVENT_DATE.plusDays(6), "Pix"));
		FinancialTransaction receipt = transactionRepository.findById(
				USER_A, partial.receiptTransactionId()).orElseThrow();

		assertEquals(TransactionKind.REIMBURSEMENT_RECEIPT, receipt.kind());
		assertEquals(TransactionSource.VOICE, receipt.source());
		assertEquals(new BigDecimal("50.00"), receipt.totalAmount().amount());
		assertEquals(REIMBURSEMENT, receipt.categoryId());
		assertEquals(ReimbursementStatus.PARTIALLY_PAID, partial.claim().status());
		assertEquals(new BigDecimal("124.00"), partial.claim().remainingAmount());

		RecordReimbursementPaymentOutput paid = paymentUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				created.claim().claimId(),
				new RecordReimbursementPaymentInput(
						new BigDecimal("124.00"), EVENT_DATE.plusDays(10), null));

		assertEquals(ReimbursementStatus.PAID, paid.claim().status());
		assertEquals(new BigDecimal("0.00"), paid.claim().remainingAmount());
		int transactionCount = transactionCount(USER_A);
		int paymentCount = paymentCount(created.claim().claimId().value());
		assertThrows(
				InvalidReimbursementInputException.class,
				() -> paymentUseCase.execute(
						USER_A,
						TransactionSource.MANUAL,
						created.claim().claimId(),
						new RecordReimbursementPaymentInput(
								new BigDecimal("0.01"), EVENT_DATE.plusDays(11), null)));
		assertEquals(transactionCount, transactionCount(USER_A));
		assertEquals(paymentCount, paymentCount(created.claim().claimId().value()));

		var summary = summaryUseCase.execute(USER_A, jon.personId());
		assertEquals(new BigDecimal("174.00"), summary.totalOriginal());
		assertEquals(new BigDecimal("174.00"), summary.totalReimbursed());
		assertEquals(new BigDecimal("0.00"), summary.totalOutstanding());
		assertEquals(0, summary.openClaimCount());
	}

	@Test
	void searchFiltersByOwnerPersonAndDerivedStatusWithCorrectPagination() {
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Jon Doe"));
		PersonOutput maria = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Maria"));
		CreateReimbursableExpenseOutput pending = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				expenseInput(jon.personId(), new BigDecimal("20.00"), null, null));
		CreateReimbursableExpenseOutput paid = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				expenseInput(maria.personId(), new BigDecimal("30.00"), null, null));
		paymentUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				paid.claim().claimId(),
				new RecordReimbursementPaymentInput(
						new BigDecimal("30.00"), EVENT_DATE, null));
		PersonOutput otherPerson = createPersonUseCase.execute(
				USER_B, new CreatePersonInput("Other"));
		createExpenseUseCase.execute(
				USER_B,
				TransactionSource.MANUAL,
				expenseInput(otherPerson.personId(), new BigDecimal("99.00"), null, null));

		var pendingPage = listUseCase.execute(
				USER_A,
				new ListReimbursementsInput(
						jon.personId(), ReimbursementStatus.PENDING, 0, 1));
		var paidPage = listUseCase.execute(
				USER_A,
				new ListReimbursementsInput(
						null, ReimbursementStatus.PAID, 0, 20));

		assertEquals(1, pendingPage.totalElements());
		assertEquals(pending.claim().claimId(), pendingPage.content().getFirst().claimId());
		assertEquals(1, pendingPage.totalPages());
		assertEquals(1, paidPage.totalElements());
		assertEquals(paid.claim().claimId(), paidPage.content().getFirst().claimId());
	}

	@Test
	void savingClaimAgainDoesNotDuplicatePaymentsAndDetectsBothLinkedTransactions() {
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Jon Doe"));
		CreateReimbursableExpenseOutput created = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				expenseInput(jon.personId(), new BigDecimal("50.00"), null, null));
		RecordReimbursementPaymentOutput paid = paymentUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				created.claim().claimId(),
				new RecordReimbursementPaymentInput(
						new BigDecimal("10.00"), EVENT_DATE, null));
		ReimbursementClaim claim = claimRepository.findById(
				USER_A, created.claim().claimId()).orElseThrow();
		assertTrue(claimRepository.findByIdForRepayment(
				USER_A, created.claim().claimId()).isPresent());
		assertFalse(claimRepository.findByIdForRepayment(
				USER_B, created.claim().claimId()).isPresent());

		claimRepository.save(claim);

		assertEquals(1, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM reimbursement_payment WHERE claim_id = ?",
				Integer.class,
				claim.id().value()));
		assertTrue(transactionRepository.isLinkedToReimbursement(
				USER_A, created.expense().transactionId()));
		assertTrue(transactionRepository.isLinkedToReimbursement(
				USER_A, paid.receiptTransactionId()));
		assertFalse(transactionRepository.isLinkedToReimbursement(
				USER_B, created.expense().transactionId()));
	}

	@Test
	void invalidClaimNoteRollsBackTheAlreadyConstructedExpense() {
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Jon Doe"));

		assertThrows(
				InvalidReimbursementInputException.class,
				() -> createExpenseUseCase.execute(
						USER_A,
						TransactionSource.MANUAL,
						expenseInput(
								jon.personId(),
								new BigDecimal("10.00"),
								null,
								"x".repeat(501))));

		assertTrue(TestTransaction.isFlaggedForRollback());
		TestTransaction.end();

		assertEquals(0, transactionCount(USER_A));
		assertEquals(0, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM reimbursement_claim WHERE user_id = ?",
				Integer.class,
				USER_A.value()));
	}

	@Test
	void databaseRejectsClaimWithAnotherUsersExpense() {
		Person person = personRepository.save(new Person(
				PersonId.generate(), USER_A, "Jon Doe"));
		PersonOutput other = createPersonUseCase.execute(
				USER_B, new CreatePersonInput("Other"));
		CreateReimbursableExpenseOutput otherExpense = createExpenseUseCase.execute(
				USER_B,
				TransactionSource.MANUAL,
				expenseInput(other.personId(), new BigDecimal("10.00"), null, null));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> jdbcTemplate.update("""
						INSERT INTO reimbursement_claim
						    (id, user_id, expense_transaction_id, person_id,
						     original_amount, currency)
						VALUES (?, ?, ?, ?, ?, ?)
						""",
						UUID.randomUUID(),
						USER_A.value(),
						otherExpense.expense().transactionId().value(),
						person.id().value(),
						new BigDecimal("10.00"),
						"BRL"));
	}

	@Test
	void databaseRejectsClaimWithAnotherUsersPerson() {
		Person otherPerson = personRepository.save(new Person(
				PersonId.generate(), USER_B, "Other"));
		FinancialTransaction expense = transactionRepository.save(
				FinancialTransaction.createSingleOccurrence(
						USER_A,
						TransactionKind.EXPENSE,
						"Expense",
						Money.brl(new BigDecimal("10.00")),
						GROCERIES,
						EVENT_DATE,
						TransactionSource.MANUAL));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> jdbcTemplate.update("""
						INSERT INTO reimbursement_claim
						    (id, user_id, expense_transaction_id, person_id,
						     original_amount, currency)
						VALUES (?, ?, ?, ?, ?, ?)
						""",
						UUID.randomUUID(),
						USER_A.value(),
						expense.id().value(),
						otherPerson.id().value(),
						new BigDecimal("10.00"),
						"BRL"));
	}

	@Test
	void databaseRejectsPaymentWithAnotherUsersReceipt() {
		PersonOutput jon = createPersonUseCase.execute(
				USER_A, new CreatePersonInput("Jon Doe"));
		CreateReimbursableExpenseOutput created = createExpenseUseCase.execute(
				USER_A,
				TransactionSource.MANUAL,
				expenseInput(jon.personId(), new BigDecimal("10.00"), null, null));
		FinancialTransaction otherReceipt = transactionRepository.save(
				FinancialTransaction.createSingleOccurrence(
						USER_B,
						TransactionKind.REIMBURSEMENT_RECEIPT,
						"Other receipt",
						Money.brl(new BigDecimal("5.00")),
						REIMBURSEMENT,
						EVENT_DATE,
						TransactionSource.MANUAL));

		assertThrows(
				DataIntegrityViolationException.class,
				() -> jdbcTemplate.update("""
						INSERT INTO reimbursement_payment
						    (id, user_id, claim_id, receipt_transaction_id,
						     amount, currency, received_date)
						VALUES (?, ?, ?, ?, ?, ?, ?)
						""",
						UUID.randomUUID(),
						USER_A.value(),
						created.claim().claimId().value(),
						otherReceipt.id().value(),
						new BigDecimal("5.00"),
						"BRL",
						EVENT_DATE));
	}

	private CreateReimbursableExpenseInput expenseInput(
			PersonId personId,
			BigDecimal amount,
			BigDecimal amountOwed,
			String note) {
		return new CreateReimbursableExpenseInput(
				"Pizza",
				amount,
				GROCERIES,
				EVENT_DATE,
				null,
				3,
				personId,
				amountOwed,
				note);
	}

	private int transactionCount(UserId ownerId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM financial_transaction WHERE user_id = ?",
				Integer.class,
				ownerId.value());
	}

	private int paymentCount(UUID claimId) {
		return jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM reimbursement_payment WHERE claim_id = ?",
				Integer.class,
				claimId);
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

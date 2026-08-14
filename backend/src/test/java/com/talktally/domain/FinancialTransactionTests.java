package com.talktally.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FinancialTransactionTests {

	private static final LocalDate EVENT_DATE = LocalDate.of(2026, 8, 14);
	private static final Currency USD = Currency.getInstance("USD");

	@Test
	void createsNormalSingleExpense() {
		FinancialTransaction transaction = createSingle(TransactionKind.EXPENSE, "Groceries");

		assertNotNull(transaction.id());
		assertEquals(TransactionKind.EXPENSE, transaction.kind());
		assertEquals("Groceries", transaction.description());
		assertEquals(1, transaction.occurrences().size());
		assertEquals(transaction.totalAmount(), transaction.occurrences().getFirst().amount());
		assertEquals(EVENT_DATE, transaction.occurrences().getFirst().effectiveDate());
	}

	@Test
	void createsNormalIncome() {
		FinancialTransaction transaction = createSingle(TransactionKind.INCOME, "Salary");

		assertEquals(TransactionKind.INCOME, transaction.kind());
	}

	@Test
	void reimbursementReceiptKindExistsIndependentlyFromIncome() {
		FinancialTransaction transaction = createSingle(
				TransactionKind.REIMBURSEMENT_RECEIPT,
				"Repayment from Jon");

		assertEquals(TransactionKind.REIMBURSEMENT_RECEIPT, transaction.kind());
		assertFalse(transaction.kind() == TransactionKind.INCOME);
	}

	@Test
	void createsValidatedInstallmentTransaction() {
		FinancialTransaction transaction = FinancialTransaction.createInstallment(
				UserId.generate(),
				TransactionKind.EXPENSE,
				"Pizza for Jon",
				Money.brl(new BigDecimal("100.00")),
				CategoryId.generate(),
				EVENT_DATE,
				TransactionSource.VOICE,
				3,
				LocalDate.of(2026, 9, 10));

		assertEquals(3, transaction.occurrences().size());
		assertEquals(Money.brl(new BigDecimal("33.34")), transaction.occurrences().getLast().amount());
		assertEquals(LocalDate.of(2026, 11, 10), transaction.occurrences().getLast().effectiveDate());
	}

	@Test
	void reconstructionPreservesIdentityAndOrdersOccurrencesBySequence() {
		TransactionId id = TransactionId.generate();
		List<TransactionOccurrence> occurrences = List.of(
				new TransactionOccurrence(2, EVENT_DATE.plusMonths(1), Money.brl(new BigDecimal("5.00"))),
				new TransactionOccurrence(1, EVENT_DATE, Money.brl(new BigDecimal("5.00"))));

		FinancialTransaction transaction = FinancialTransaction.reconstruct(
				id,
				UserId.generate(),
				TransactionKind.EXPENSE,
				"Reconstructed",
				Money.brl(new BigDecimal("10.00")),
				CategoryId.generate(),
				EVENT_DATE,
				TransactionSource.MANUAL,
				occurrences);

		assertEquals(id, transaction.id());
		assertEquals(List.of(1, 2), transaction.occurrences().stream()
				.map(TransactionOccurrence::sequenceNumber)
				.toList());
	}

	@Test
	void rejectsOccurrenceSumMismatchDuringReconstruction() {
		List<TransactionOccurrence> occurrences = List.of(
				new TransactionOccurrence(1, EVENT_DATE, Money.brl(new BigDecimal("9.99"))));

		assertThrows(IllegalArgumentException.class, () -> reconstruct(
				"Mismatch",
				Money.brl(new BigDecimal("10.00")),
				occurrences));
	}

	@Test
	void rejectsMismatchedOccurrenceCurrency() {
		List<TransactionOccurrence> occurrences = List.of(
				new TransactionOccurrence(
						1,
						EVENT_DATE,
						Money.of(new BigDecimal("10.00"), USD)));

		assertThrows(IllegalArgumentException.class, () -> reconstruct(
				"Currency mismatch",
				Money.brl(new BigDecimal("10.00")),
				occurrences));
	}

	@Test
	void rejectsBlankDescription() {
		assertThrows(IllegalArgumentException.class, () -> createSingle(TransactionKind.EXPENSE, "  \t"));
	}

	@Test
	void rejectsZeroTransactionAmount() {
		assertThrows(IllegalArgumentException.class, () -> FinancialTransaction.createSingleOccurrence(
				UserId.generate(),
				TransactionKind.EXPENSE,
				"Zero",
				Money.brl(BigDecimal.ZERO),
				CategoryId.generate(),
				EVENT_DATE,
				TransactionSource.MANUAL));
	}

	@Test
	void rejectsMissingOrDuplicateOccurrenceSequences() {
		List<TransactionOccurrence> missingSequence = List.of(
				new TransactionOccurrence(1, EVENT_DATE, Money.brl(new BigDecimal("5.00"))),
				new TransactionOccurrence(3, EVENT_DATE.plusMonths(1), Money.brl(new BigDecimal("5.00"))));
		List<TransactionOccurrence> duplicateSequence = List.of(
				new TransactionOccurrence(1, EVENT_DATE, Money.brl(new BigDecimal("5.00"))),
				new TransactionOccurrence(1, EVENT_DATE.plusMonths(1), Money.brl(new BigDecimal("5.00"))));

		assertThrows(IllegalArgumentException.class, () -> reconstruct(
				"Missing sequence",
				Money.brl(new BigDecimal("10.00")),
				missingSequence));
		assertThrows(IllegalArgumentException.class, () -> reconstruct(
				"Duplicate sequence",
				Money.brl(new BigDecimal("10.00")),
				duplicateSequence));
	}

	@Test
	void rejectsEmptyOccurrenceCollection() {
		assertThrows(IllegalArgumentException.class, () -> reconstruct(
				"No occurrences",
				Money.brl(new BigDecimal("10.00")),
				List.of()));
	}

	@Test
	void defensivelyCopiesAndExposesImmutableOccurrences() {
		List<TransactionOccurrence> suppliedOccurrences = new ArrayList<>();
		suppliedOccurrences.add(new TransactionOccurrence(
				1,
				EVENT_DATE,
				Money.brl(new BigDecimal("10.00"))));

		FinancialTransaction transaction = reconstruct(
				"Immutable occurrences",
				Money.brl(new BigDecimal("10.00")),
				suppliedOccurrences);
		suppliedOccurrences.clear();

		assertEquals(1, transaction.occurrences().size());
		assertThrows(UnsupportedOperationException.class, () -> transaction.occurrences().clear());
	}

	@Test
	void rejectsNullRequiredValues() {
		Money amount = Money.brl(new BigDecimal("10.00"));
		UserId ownerId = UserId.generate();
		CategoryId categoryId = CategoryId.generate();

		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				null, TransactionKind.EXPENSE, "Expense", amount, categoryId, EVENT_DATE, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, null, "Expense", amount, categoryId, EVENT_DATE, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, TransactionKind.EXPENSE, null, amount, categoryId, EVENT_DATE, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, TransactionKind.EXPENSE, "Expense", null, categoryId, EVENT_DATE, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, TransactionKind.EXPENSE, "Expense", amount, null, EVENT_DATE, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, TransactionKind.EXPENSE, "Expense", amount, categoryId, null, TransactionSource.MANUAL));
		assertThrows(NullPointerException.class, () -> FinancialTransaction.createSingleOccurrence(
				ownerId, TransactionKind.EXPENSE, "Expense", amount, categoryId, EVENT_DATE, null));
	}

	private static FinancialTransaction createSingle(TransactionKind kind, String description) {
		return FinancialTransaction.createSingleOccurrence(
				UserId.generate(),
				kind,
				description,
				Money.brl(new BigDecimal("10.00")),
				CategoryId.generate(),
				EVENT_DATE,
				TransactionSource.MANUAL);
	}

	private static FinancialTransaction reconstruct(
			String description,
			Money total,
			List<TransactionOccurrence> occurrences) {
		return FinancialTransaction.reconstruct(
				TransactionId.generate(),
				UserId.generate(),
				TransactionKind.EXPENSE,
				description,
				total,
				CategoryId.generate(),
				EVENT_DATE,
				TransactionSource.MANUAL,
				occurrences);
	}
}

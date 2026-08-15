package com.talktally.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReimbursementDomainTests {

	private static final UserId USER = UserId.generate();
	private static final PersonId PERSON = PersonId.generate();
	private static final TransactionId EXPENSE = TransactionId.generate();
	private static final LocalDate DATE = LocalDate.of(2026, 8, 20);

	@Test
	void personTrimsDisplayNameAndNormalizesDeterministically() {
		Person person = new Person(PersonId.generate(), USER, "  Jon   Doe  ");

		assertEquals("Jon   Doe", person.displayName());
		assertEquals("jon doe", person.normalizedName());
		assertEquals("jon doe", Person.normalizeName(" JON\t Doe "));
	}

	@Test
	void personRejectsBlankAndOversizedNames() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new Person(PersonId.generate(), USER, "   "));
		assertThrows(
				IllegalArgumentException.class,
				() -> new Person(PersonId.generate(), USER, "x".repeat(121)));
	}

	@Test
	void claimMovesFromPendingThroughPartialToPaid() {
		ReimbursementClaim pending = claim("174.00");
		ReimbursementClaim partial = pending.addPayment(payment("50.00", DATE));
		ReimbursementClaim paid = partial.addPayment(payment("124.00", DATE.plusDays(1)));

		assertEquals(ReimbursementStatus.PENDING, pending.status());
		assertEquals(new BigDecimal("174.00"), pending.remainingAmount().amount());
		assertEquals(ReimbursementStatus.PARTIALLY_PAID, partial.status());
		assertEquals(new BigDecimal("50.00"), partial.amountReimbursed().amount());
		assertEquals(new BigDecimal("124.00"), partial.remainingAmount().amount());
		assertEquals(ReimbursementStatus.PAID, paid.status());
		assertEquals(new BigDecimal("174.00"), paid.amountReimbursed().amount());
		assertEquals(new BigDecimal("0.00"), paid.remainingAmount().amount());
	}

	@Test
	void claimRejectsOverpaymentAndCurrencyMismatch() {
		ReimbursementClaim claim = claim("100.00");

		assertThrows(
				IllegalArgumentException.class,
				() -> claim.addPayment(payment("100.01", DATE)));
		assertThrows(
				IllegalArgumentException.class,
				() -> claim.addPayment(new ReimbursementPayment(
						ReimbursementPaymentId.generate(),
						Money.of(new BigDecimal("10.00"), Currency.getInstance("USD")),
						DATE,
						TransactionId.generate(),
						null)));
	}

	@Test
	void reconstructionSortsAndDefensivelyCopiesPayments() {
		ReimbursementPayment later = payment("20.00", DATE.plusDays(1));
		ReimbursementPayment earlier = payment("10.00", DATE);
		List<ReimbursementPayment> source = new ArrayList<>(List.of(later, earlier));

		ReimbursementClaim reconstructed = ReimbursementClaim.reconstruct(
				ReimbursementClaimId.generate(),
				USER,
				EXPENSE,
				PERSON,
				Money.brl(new BigDecimal("100.00")),
				" note ",
				source);
		source.clear();

		assertEquals(List.of(earlier, later), reconstructed.payments());
		assertEquals("note", reconstructed.note());
		assertThrows(
				UnsupportedOperationException.class,
				() -> reconstructed.payments().clear());
	}

	@Test
	void paymentRejectsZeroNegativeAndMissingRelationships() {
		assertThrows(
				IllegalArgumentException.class,
				() -> new ReimbursementPayment(
						ReimbursementPaymentId.generate(),
						Money.brl(BigDecimal.ZERO),
						DATE,
						TransactionId.generate(),
						null));
		assertThrows(
				IllegalArgumentException.class,
				() -> Money.brl(new BigDecimal("-1.00")));
		assertThrows(
				NullPointerException.class,
				() -> new ReimbursementPayment(
						ReimbursementPaymentId.generate(),
						Money.brl(BigDecimal.ONE),
						null,
						TransactionId.generate(),
						null));
		assertThrows(
				NullPointerException.class,
				() -> new ReimbursementPayment(
						ReimbursementPaymentId.generate(),
						Money.brl(BigDecimal.ONE),
						DATE,
						null,
						null));
	}

	private static ReimbursementClaim claim(String amount) {
		return ReimbursementClaim.create(
				USER,
				EXPENSE,
				PERSON,
				Money.brl(new BigDecimal(amount)),
				null);
	}

	private static ReimbursementPayment payment(String amount, LocalDate date) {
		return new ReimbursementPayment(
				ReimbursementPaymentId.generate(),
				Money.brl(new BigDecimal(amount)),
				date,
				TransactionId.from(UUID.randomUUID()),
				null);
	}
}

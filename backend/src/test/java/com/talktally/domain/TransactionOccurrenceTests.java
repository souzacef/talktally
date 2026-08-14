package com.talktally.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TransactionOccurrenceTests {

	@Test
	void createsPositiveOccurrenceStartingAtSequenceOne() {
		LocalDate date = LocalDate.of(2026, 8, 14);
		Money amount = Money.brl(new BigDecimal("25.00"));

		TransactionOccurrence occurrence = new TransactionOccurrence(1, date, amount);

		assertEquals(1, occurrence.sequenceNumber());
		assertEquals(date, occurrence.effectiveDate());
		assertEquals(amount, occurrence.amount());
	}

	@Test
	void rejectsNonPositiveSequenceNumber() {
		assertThrows(IllegalArgumentException.class, () -> new TransactionOccurrence(
				0,
				LocalDate.of(2026, 8, 14),
				Money.brl(BigDecimal.ONE)));
	}

	@Test
	void rejectsZeroAmount() {
		assertThrows(IllegalArgumentException.class, () -> new TransactionOccurrence(
				1,
				LocalDate.of(2026, 8, 14),
				Money.brl(BigDecimal.ZERO)));
	}

	@Test
	void rejectsNullDateAndAmount() {
		assertThrows(NullPointerException.class, () -> new TransactionOccurrence(
				1,
				null,
				Money.brl(BigDecimal.ONE)));
		assertThrows(NullPointerException.class, () -> new TransactionOccurrence(
				1,
				LocalDate.of(2026, 8, 14),
				null));
	}
}

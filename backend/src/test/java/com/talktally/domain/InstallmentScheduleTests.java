package com.talktally.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InstallmentScheduleTests {

	@Test
	void allocatesOneHundredSeventyFourReaisIntoThreeEqualInstallments() {
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("174.00")),
				3,
				LocalDate.of(2026, 8, 14));

		assertEquals(
				List.of(
						Money.brl(new BigDecimal("58.00")),
						Money.brl(new BigDecimal("58.00")),
						Money.brl(new BigDecimal("58.00"))),
				schedule.stream().map(TransactionOccurrence::amount).toList());
	}

	@Test
	void putsCentRemainderIntoLastInstallment() {
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("100.00")),
				3,
				LocalDate.of(2026, 8, 14));

		assertEquals(
				List.of(
						Money.brl(new BigDecimal("33.33")),
						Money.brl(new BigDecimal("33.33")),
						Money.brl(new BigDecimal("33.34"))),
				schedule.stream().map(TransactionOccurrence::amount).toList());
	}

	@Test
	void createsMonthlyEffectiveDatesFromOriginalAnchorDate() {
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("50.00")),
				5,
				LocalDate.of(2026, 1, 31));

		assertEquals(
				List.of(
						LocalDate.of(2026, 1, 31),
						LocalDate.of(2026, 2, 28),
						LocalDate.of(2026, 3, 31),
						LocalDate.of(2026, 4, 30),
						LocalDate.of(2026, 5, 31)),
				schedule.stream().map(TransactionOccurrence::effectiveDate).toList());
	}

	@Test
	void preservesTheOriginalMonthEndAnchorAcrossALeapYear() {
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("30.00")),
				3,
				LocalDate.of(2024, 1, 31));

		assertEquals(
				List.of(
						LocalDate.of(2024, 1, 31),
						LocalDate.of(2024, 2, 29),
						LocalDate.of(2024, 3, 31)),
				schedule.stream().map(TransactionOccurrence::effectiveDate).toList());
	}

	@Test
	void supportsInstallmentCountOne() {
		LocalDate date = LocalDate.of(2026, 8, 14);
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("10.00")),
				1,
				date);

		assertEquals(List.of(new TransactionOccurrence(1, date, Money.brl(new BigDecimal("10.00")))), schedule);
	}

	@Test
	void rejectsInstallmentCountZero() {
		assertThrows(IllegalArgumentException.class, () -> InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("10.00")),
				0,
				LocalDate.of(2026, 8, 14)));
	}

	@Test
	void rejectsMoreInstallmentsThanAvailablePositiveCents() {
		assertThrows(IllegalArgumentException.class, () -> InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("0.02")),
				3,
				LocalDate.of(2026, 8, 14)));
	}

	@Test
	void rejectsZeroTotal() {
		assertThrows(IllegalArgumentException.class, () -> InstallmentSchedule.allocate(
				Money.zero(Money.brl(BigDecimal.ZERO).currency()),
				1,
				LocalDate.of(2026, 8, 14)));
	}

	@Test
	void returnsImmutableSchedule() {
		List<TransactionOccurrence> schedule = InstallmentSchedule.allocate(
				Money.brl(new BigDecimal("10.00")),
				1,
				LocalDate.of(2026, 8, 14));

		assertThrows(UnsupportedOperationException.class, () -> schedule.clear());
	}
}

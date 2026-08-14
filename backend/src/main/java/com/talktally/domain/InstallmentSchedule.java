package com.talktally.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class InstallmentSchedule {

	private InstallmentSchedule() {
	}

	public static List<TransactionOccurrence> allocate(
			Money total,
			int installmentCount,
			LocalDate firstEffectiveDate) {
		Objects.requireNonNull(total, "total must not be null");
		Objects.requireNonNull(firstEffectiveDate, "first effective date must not be null");
		if (!total.isPositive()) {
			throw new IllegalArgumentException("total must be greater than zero");
		}
		if (installmentCount < 1) {
			throw new IllegalArgumentException("installment count must be at least 1");
		}

		int fractionDigits = total.amount().scale();
		BigInteger totalMinorUnits = total.amount()
				.movePointRight(fractionDigits)
				.toBigIntegerExact();
		BigInteger count = BigInteger.valueOf(installmentCount);
		BigInteger[] division = totalMinorUnits.divideAndRemainder(count);

		if (division[0].signum() == 0) {
			throw new IllegalArgumentException(
					"total cannot be split into the requested number of positive installments");
		}

		List<TransactionOccurrence> occurrences = new ArrayList<>(installmentCount);
		for (int index = 0; index < installmentCount; index++) {
			BigInteger installmentMinorUnits = division[0];
			if (index == installmentCount - 1) {
				installmentMinorUnits = installmentMinorUnits.add(division[1]);
			}

			Money amount = Money.of(
					new BigDecimal(installmentMinorUnits, fractionDigits),
					total.currency());
			occurrences.add(new TransactionOccurrence(
					index + 1,
					firstEffectiveDate.plusMonths(index),
					amount));
		}

		return List.copyOf(occurrences);
	}
}

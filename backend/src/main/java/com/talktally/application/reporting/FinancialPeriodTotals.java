package com.talktally.application.reporting;

import java.math.BigDecimal;
import java.util.Objects;

public record FinancialPeriodTotals(
		BigDecimal earnedIncome,
		BigDecimal expenses,
		BigDecimal reimbursementsReceived,
		long occurrenceCount,
		long transactionCount) {

	public FinancialPeriodTotals {
		Objects.requireNonNull(earnedIncome, "earned income must not be null");
		Objects.requireNonNull(expenses, "expenses must not be null");
		Objects.requireNonNull(reimbursementsReceived, "reimbursements must not be null");
		if (occurrenceCount < 0 || transactionCount < 0) {
			throw new IllegalArgumentException("reporting counts must not be negative");
		}
	}
}

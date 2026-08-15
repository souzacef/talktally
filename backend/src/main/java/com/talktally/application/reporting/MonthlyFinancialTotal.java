package com.talktally.application.reporting;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;

public record MonthlyFinancialTotal(
		YearMonth month,
		BigDecimal earnedIncome,
		BigDecimal expenses,
		BigDecimal reimbursementsReceived) {

	public MonthlyFinancialTotal {
		Objects.requireNonNull(month, "month must not be null");
		Objects.requireNonNull(earnedIncome, "earned income must not be null");
		Objects.requireNonNull(expenses, "expenses must not be null");
		Objects.requireNonNull(reimbursementsReceived, "reimbursements must not be null");
	}
}

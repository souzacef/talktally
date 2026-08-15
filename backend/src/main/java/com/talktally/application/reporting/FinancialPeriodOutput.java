package com.talktally.application.reporting;

import java.math.BigDecimal;

public record FinancialPeriodOutput(
		BigDecimal earnedIncome,
		BigDecimal expenses,
		BigDecimal reimbursementsReceived,
		BigDecimal netCashFlow,
		long occurrenceCount,
		long transactionCount) {
}

package com.talktally.application.reporting;

import java.math.BigDecimal;

public record MonthlyCashFlowBucketOutput(
		int year,
		int month,
		BigDecimal earnedIncome,
		BigDecimal expenses,
		BigDecimal reimbursementsReceived,
		BigDecimal netCashFlow) {
}

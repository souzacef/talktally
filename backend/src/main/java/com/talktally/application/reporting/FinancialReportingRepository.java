package com.talktally.application.reporting;

import com.talktally.domain.TransactionKind;
import com.talktally.domain.UserId;

import java.time.LocalDate;
import java.util.List;

public interface FinancialReportingRepository {

	FinancialPeriodTotals summarize(UserId ownerId, LocalDate from, LocalDate to);

	List<CategoryFinancialTotal> categoryBreakdown(
			UserId ownerId,
			LocalDate from,
			LocalDate to,
			TransactionKind kind);

	List<MonthlyFinancialTotal> monthlyCashFlow(
			UserId ownerId,
			LocalDate from,
			LocalDate to);
}

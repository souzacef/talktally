package com.talktally.infrastructure.web.dashboard;

import com.talktally.application.reporting.FinancialSummaryOutput;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FinancialSummaryResponse(
		LocalDate from,
		LocalDate to,
		String currency,
		PeriodResponse period,
		OwedToMeResponse owedToMe) {

	static FinancialSummaryResponse from(FinancialSummaryOutput output) {
		return new FinancialSummaryResponse(
				output.from(),
				output.to(),
				output.currency(),
				new PeriodResponse(
						output.period().earnedIncome(),
						output.period().expenses(),
						output.period().reimbursementsReceived(),
						output.period().netCashFlow(),
						output.period().occurrenceCount(),
						output.period().transactionCount()),
				new OwedToMeResponse(
						output.owedToMe().outstanding(),
						output.owedToMe().openClaims()));
	}

	public record PeriodResponse(
			BigDecimal earnedIncome,
			BigDecimal expenses,
			BigDecimal reimbursementsReceived,
			BigDecimal netCashFlow,
			long occurrenceCount,
			long transactionCount) {
	}

	public record OwedToMeResponse(BigDecimal outstanding, long openClaims) {
	}
}

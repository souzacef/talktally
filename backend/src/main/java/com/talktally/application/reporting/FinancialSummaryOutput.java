package com.talktally.application.reporting;

import java.time.LocalDate;

public record FinancialSummaryOutput(
		LocalDate from,
		LocalDate to,
		String currency,
		FinancialPeriodOutput period,
		OwedToMeSnapshotOutput owedToMe) {
}

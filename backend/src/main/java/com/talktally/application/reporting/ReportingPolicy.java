package com.talktally.application.reporting;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;

public final class ReportingPolicy {

	public static final int MAX_MONTH_BUCKETS = 60;

	private ReportingPolicy() {
	}

	public static void requireValidRange(LocalDate from, LocalDate to) {
		if (from == null || to == null) {
			throw new InvalidReportingInputException("from and to dates are required");
		}
		if (from.isAfter(to)) {
			throw new InvalidReportingInputException("from date must not be after to date");
		}
	}

	public static void requireValidMonthlyRange(LocalDate from, LocalDate to) {
		requireValidRange(from, to);
		long months = ChronoUnit.MONTHS.between(YearMonth.from(from), YearMonth.from(to)) + 1;
		if (months > MAX_MONTH_BUCKETS) {
			throw new InvalidReportingInputException(
					"monthly reporting range must not exceed " + MAX_MONTH_BUCKETS + " months");
		}
	}
}

package com.talktally.infrastructure.web.dashboard;

import com.talktally.application.reporting.MonthlyCashFlowOutput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MonthlyCashFlowResponse(
		LocalDate from,
		LocalDate to,
		String currency,
		List<MonthlyBucketResponse> buckets) {

	public MonthlyCashFlowResponse {
		buckets = List.copyOf(buckets);
	}

	static MonthlyCashFlowResponse from(MonthlyCashFlowOutput output) {
		return new MonthlyCashFlowResponse(
				output.from(),
				output.to(),
				output.currency(),
				output.buckets().stream()
						.map(bucket -> new MonthlyBucketResponse(
								bucket.year(),
								bucket.month(),
								bucket.earnedIncome(),
								bucket.expenses(),
								bucket.reimbursementsReceived(),
								bucket.netCashFlow()))
						.toList());
	}

	public record MonthlyBucketResponse(
			int year,
			int month,
			BigDecimal earnedIncome,
			BigDecimal expenses,
			BigDecimal reimbursementsReceived,
			BigDecimal netCashFlow) {
	}
}

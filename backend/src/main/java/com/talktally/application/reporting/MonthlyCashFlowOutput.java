package com.talktally.application.reporting;

import java.time.LocalDate;
import java.util.List;

public record MonthlyCashFlowOutput(
		LocalDate from,
		LocalDate to,
		String currency,
		List<MonthlyCashFlowBucketOutput> buckets) {

	public MonthlyCashFlowOutput {
		buckets = List.copyOf(buckets);
	}
}

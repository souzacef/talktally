package com.talktally.infrastructure.web.person;

import com.talktally.application.person.output.PersonReimbursementSummaryOutput;

import java.math.BigDecimal;
import java.util.UUID;

public record PersonReimbursementSummaryResponse(
		UUID personId,
		String displayName,
		BigDecimal totalOriginal,
		BigDecimal totalReimbursed,
		BigDecimal totalOutstanding,
		String currency,
		long openClaimCount) {

	static PersonReimbursementSummaryResponse from(
			PersonReimbursementSummaryOutput output) {
		return new PersonReimbursementSummaryResponse(
				output.personId().value(),
				output.displayName(),
				output.totalOriginal(),
				output.totalReimbursed(),
				output.totalOutstanding(),
				output.currency(),
				output.openClaimCount());
	}
}

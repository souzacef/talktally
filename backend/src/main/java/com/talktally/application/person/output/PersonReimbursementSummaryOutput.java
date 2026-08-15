package com.talktally.application.person.output;

import com.talktally.domain.PersonId;

import java.math.BigDecimal;

public record PersonReimbursementSummaryOutput(
		PersonId personId,
		String displayName,
		BigDecimal totalOriginal,
		BigDecimal totalReimbursed,
		BigDecimal totalOutstanding,
		String currency,
		long openClaimCount) {
}

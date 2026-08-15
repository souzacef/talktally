package com.talktally.infrastructure.web.transaction;

import com.talktally.application.output.TransactionOccurrenceOutput;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionOccurrenceResponse(
		int sequenceNumber,
		LocalDate effectiveDate,
		BigDecimal amount,
		String currency) {

	static TransactionOccurrenceResponse from(TransactionOccurrenceOutput output) {
		return new TransactionOccurrenceResponse(
				output.sequenceNumber(),
				output.effectiveDate(),
				output.amount(),
				output.currency());
	}
}

package com.talktally.infrastructure.web.transaction;

import com.talktally.application.output.TransactionOutput;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record TransactionResponse(
		UUID id,
		TransactionKind kind,
		String description,
		BigDecimal amount,
		String currency,
		UUID categoryId,
		LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		TransactionSource source,
		int installmentCount,
		boolean managedByReimbursement,
		List<TransactionOccurrenceResponse> occurrences) {

	public TransactionResponse {
		occurrences = List.copyOf(occurrences);
	}

	public static TransactionResponse from(TransactionOutput output) {
		return new TransactionResponse(
				output.transactionId().value(),
				output.kind(),
				output.description(),
				output.amount(),
				output.currency(),
				output.categoryId().value(),
				output.eventDate(),
				output.firstOccurrenceDate(),
				output.source(),
				output.installmentCount(),
				output.managedByReimbursement(),
				output.occurrences().stream()
						.map(TransactionOccurrenceResponse::from)
						.toList());
	}
}

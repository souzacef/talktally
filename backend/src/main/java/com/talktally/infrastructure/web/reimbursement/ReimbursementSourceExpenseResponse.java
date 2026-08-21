package com.talktally.infrastructure.web.reimbursement;

import com.talktally.application.reimbursement.output.ReimbursementSourceExpenseOutput;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ReimbursementSourceExpenseResponse(
		UUID transactionId,
		String description,
		BigDecimal amount,
		String currency,
		UUID categoryId,
		LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		int installmentCount) {

	static ReimbursementSourceExpenseResponse from(
			ReimbursementSourceExpenseOutput output) {
		return new ReimbursementSourceExpenseResponse(
				output.transactionId().value(),
				output.description(),
				output.amount(),
				output.currency(),
				output.categoryId().value(),
				output.eventDate(),
				output.firstOccurrenceDate(),
				output.installmentCount());
	}
}

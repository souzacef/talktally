package com.talktally.application.reimbursement.output;

import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record ReimbursementSourceExpenseOutput(
		TransactionId transactionId,
		String description,
		BigDecimal amount,
		String currency,
		CategoryId categoryId,
		LocalDate eventDate,
		LocalDate firstOccurrenceDate,
		int installmentCount) {

	public ReimbursementSourceExpenseOutput {
		Objects.requireNonNull(transactionId, "transaction id must not be null");
		Objects.requireNonNull(description, "description must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		Objects.requireNonNull(categoryId, "category id must not be null");
		Objects.requireNonNull(eventDate, "event date must not be null");
		Objects.requireNonNull(firstOccurrenceDate, "first occurrence date must not be null");
		if (installmentCount < 1) {
			throw new IllegalArgumentException("installment count must be positive");
		}
	}
}

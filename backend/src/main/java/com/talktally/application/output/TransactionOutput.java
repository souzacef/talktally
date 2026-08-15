package com.talktally.application.output;

import com.talktally.domain.CategoryId;
import com.talktally.domain.TransactionId;
import com.talktally.domain.TransactionKind;
import com.talktally.domain.TransactionSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public record TransactionOutput(
		TransactionId transactionId,
		TransactionKind kind,
		String description,
		BigDecimal amount,
		String currency,
		CategoryId categoryId,
		LocalDate eventDate,
		TransactionSource source,
		int installmentCount,
		List<TransactionOccurrenceOutput> occurrences) {

	public TransactionOutput {
		Objects.requireNonNull(transactionId, "transaction id must not be null");
		Objects.requireNonNull(kind, "kind must not be null");
		Objects.requireNonNull(description, "description must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
		Objects.requireNonNull(categoryId, "category id must not be null");
		Objects.requireNonNull(eventDate, "event date must not be null");
		Objects.requireNonNull(source, "source must not be null");
		Objects.requireNonNull(occurrences, "occurrences must not be null");
		occurrences = List.copyOf(occurrences);
		if (installmentCount != occurrences.size()) {
			throw new IllegalArgumentException("installment count must match occurrences");
		}
	}
}

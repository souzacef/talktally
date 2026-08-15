package com.talktally.application.output;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record TransactionOccurrenceOutput(
		int sequenceNumber,
		LocalDate effectiveDate,
		BigDecimal amount,
		String currency) {

	public TransactionOccurrenceOutput {
		if (sequenceNumber < 1) {
			throw new IllegalArgumentException("sequence number must be at least 1");
		}
		Objects.requireNonNull(effectiveDate, "effective date must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		Objects.requireNonNull(currency, "currency must not be null");
	}
}

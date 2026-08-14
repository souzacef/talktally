package com.talktally.domain;

import java.time.LocalDate;
import java.util.Objects;

public record TransactionOccurrence(int sequenceNumber, LocalDate effectiveDate, Money amount) {

	public TransactionOccurrence {
		if (sequenceNumber < 1) {
			throw new IllegalArgumentException("sequence number must be at least 1");
		}
		Objects.requireNonNull(effectiveDate, "effective date must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		if (!amount.isPositive()) {
			throw new IllegalArgumentException("occurrence amount must be greater than zero");
		}
	}
}

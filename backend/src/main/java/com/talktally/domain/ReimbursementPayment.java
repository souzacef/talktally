package com.talktally.domain;

import java.time.LocalDate;
import java.util.Objects;

public record ReimbursementPayment(
		ReimbursementPaymentId id,
		Money amount,
		LocalDate receivedDate,
		TransactionId receiptTransactionId,
		String note) {

	public static final int MAX_NOTE_LENGTH = 500;

	public ReimbursementPayment {
		Objects.requireNonNull(id, "id must not be null");
		Objects.requireNonNull(amount, "amount must not be null");
		if (!amount.isPositive()) {
			throw new IllegalArgumentException("payment amount must be greater than zero");
		}
		Objects.requireNonNull(receivedDate, "received date must not be null");
		Objects.requireNonNull(receiptTransactionId, "receipt transaction id must not be null");
		note = normalizeNote(note);
	}

	static String normalizeNote(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String normalized = value.strip();
		if (normalized.length() > MAX_NOTE_LENGTH) {
			throw new IllegalArgumentException(
					"note must not exceed " + MAX_NOTE_LENGTH + " characters");
		}
		return normalized;
	}
}

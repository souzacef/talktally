package com.talktally.application.exception;

import com.talktally.domain.TransactionId;

public final class TransactionNotFoundException extends RuntimeException {

	public TransactionNotFoundException(TransactionId transactionId) {
		super("transaction not found: " + transactionId.value());
	}
}

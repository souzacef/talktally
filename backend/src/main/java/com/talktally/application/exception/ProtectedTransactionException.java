package com.talktally.application.exception;

public final class ProtectedTransactionException extends RuntimeException {

	public ProtectedTransactionException() {
		super("transaction is linked to reimbursement data and cannot be modified");
	}
}

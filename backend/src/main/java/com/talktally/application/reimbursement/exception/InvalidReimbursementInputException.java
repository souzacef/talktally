package com.talktally.application.reimbursement.exception;

public final class InvalidReimbursementInputException extends RuntimeException {

	public InvalidReimbursementInputException(String message) {
		super(message);
	}

	public InvalidReimbursementInputException(String message, Throwable cause) {
		super(message, cause);
	}
}

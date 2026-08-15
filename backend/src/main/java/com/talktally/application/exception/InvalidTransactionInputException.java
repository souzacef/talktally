package com.talktally.application.exception;

public class InvalidTransactionInputException extends RuntimeException {

	public InvalidTransactionInputException(String message) {
		super(message);
	}

	public InvalidTransactionInputException(String message, Throwable cause) {
		super(message, cause);
	}
}

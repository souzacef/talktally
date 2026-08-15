package com.talktally.application.person.exception;

public final class InvalidPersonInputException extends RuntimeException {

	public InvalidPersonInputException(String message) {
		super(message);
	}

	public InvalidPersonInputException(String message, Throwable cause) {
		super(message, cause);
	}
}
